package com.huawei.codecraft.zone;

import com.huawei.codecraft.Util;
import com.huawei.codecraft.core.Berth;
import com.huawei.codecraft.core.Good;
import com.huawei.codecraft.core.Robot;
import com.huawei.codecraft.util.Point;
import com.huawei.codecraft.util.UnionFind;
import com.huawei.codecraft.way.Path;

import java.util.*;
import java.util.stream.Collectors;

import static com.huawei.codecraft.Const.*;
import static com.huawei.codecraft.Util.printDebug;
import static com.huawei.codecraft.Util.printLog;
import static com.huawei.codecraft.way.Mapinfo.isValid;
import static com.huawei.codecraft.way.Mapinfo.map;

/**
 * ClassName: RegionManager
 * Package: com.huawei.codecraft.way
 * Description: 管理region，向上提供接口
 */
public class RegionManager {
    public static final List<Region> regions = new ArrayList<>();    // 地图所有的region
    public static final Set<Point> pointSet= new HashSet<>();    // 点的set集合
    public static final Map<Point, Region> pointRegionMap = new HashMap<>();    // point 到 region 的映射，查找点属于的 region
    public static final Map<Point, Berth> pointBerthMap = new HashMap<>();  // 获取离点最近的泊位
    private final Path pathFinder;  // Path 接口的引用

    /**
     * 构造函数
     */
    public RegionManager(Path pathFinder) {
        this.pathFinder = pathFinder;  // 通过构造器注入 Path 实现
//        createInitialRegions();
//        getFullPathsFromPoints2Berths();
//        initGlobalPoint2ClosestBerthMap();
//        splitRegions();
//        assignRobotsToZone();
//        assignRobotsToRegion(); // 给区域分配机器人
//        printAll();

    }

    /**
     * 接口函数定义位置，向上提供接口
     */
    public Map<Point, Region> getPointRegionMap() {
        return pointRegionMap;
    }

    public Map<Point, Berth> getGlobalPointToClosestBerth() {
        return pointBerthMap;
    }

    public List<Region> getRegions() {
        return regions;
    }

    public void testCreateInitialRegions() {
        createInitialRegions();
    }

    public void testGetFullPathsFromPoints2Berths() {
        getFullPathsFromPoints2Berths();
    }

    public void testGlobalPoint2ClosestBerthMap() {
        initGlobalPoint2ClosestBerthMap();
    }

    public void testSplitRegions() {
        splitRegions();
    }
    /**
     * @function 创建初始的连通区域，根据地图的联通性得到最初的连通区域，同时得到所有的连通点pointRegionMap
     */
    private void createInitialRegions() {
        for (Berth berth : berths) {
            Point berthPoint = berth.pos;
            if (!pointSet.contains(berthPoint)) {
                Region region = new Region(regions.size());
                exploreMap(berthPoint.x, berthPoint.y, region);
                if (!region.getAccessiblePoints().isEmpty()) {
                    regions.add(region);
                }
            }
        }
    }

    // 开始探索地图，获取最初的region
    private void exploreMap(int startX, int startY, Region region) {
        Queue<Point> queue = new LinkedList<>();
        queue.add(new Point(startX, startY));

        while (!queue.isEmpty()) {
            Point current = queue.poll();
            int x = current.x;
            int y = current.y;

            // 跳过无效点或已经探索的点
            if (!isValid(x, y) || map[x][y] != 0 || pointSet.contains(current)) {
                continue;
            }

            // 标记点为已探索，添加到区域中
            pointSet.add(current);
            region.addAccessiblePoint(current);

            // 如果该点是泊位，也添加到区域
            Berth berth = pointToBerth.get(current);
            if (berth != null) {
                region.addBerth(berth);
            }

            // 将相邻的点加入队列
            queue.add(new Point(x + 1, y));
            queue.add(new Point(x - 1, y));
            queue.add(new Point(x, y + 1));
            queue.add(new Point(x, y - 1));
        }
    }

    /**
     * @function 获取所有的点到泊位的路径
     */
    private void getFullPathsFromPoints2Berths() {
        for (Region originalRegion : regions) {
            for (Berth berth : originalRegion.getBerths()) {
                bfsFromBerth(berth);
            }
        }
    }

    // 从泊位开始bfs进行扩散，保留最短的路径
    private void bfsFromBerth(Berth berth) {
        Queue<Point> queue = new LinkedList<>();
        Point start = berth.pos;
        queue.add(start);

        Map<Point, List<Point>> visitedPaths = new HashMap<>();
        visitedPaths.put(start, new ArrayList<>(Collections.singletonList(start)));

        boolean[][] visited = new boolean[mapWidth][mapWidth];
        visited[start.x][start.y] = true;

        while (!queue.isEmpty()) {
            Point current = queue.poll();
            List<Point> currentPath = visitedPaths.get(current);

            for (int[] dir : new int[][]{{0, 1}, {0, -1}, {-1, 0}, {1, 0}}) {
                int newX = current.x + dir[0];
                int newY = current.y + dir[1];
                Point nextPoint = new Point(newX, newY);

                if (pointSet.contains(nextPoint) && !visited[newX][newY]) {
                    visited[newX][newY] = true;

                    List<Point> newPath = new ArrayList<>(currentPath);
                    newPath.add(nextPoint);

                    visitedPaths.put(nextPoint, newPath);
                    queue.add(nextPoint);  // 将新探索点加入队列
                }
            }
        }

        // 将遍历结果更新到泊位的路径映射中
        berth.mapPath.putAll(visitedPaths);
    }

    /**
     * @function 初始化 globalPointToClosestBerth hash表，同时更新berth所拥有的points
     */
    private  void initGlobalPoint2ClosestBerthMap() {
        for (Region originalRegion : regions) {
            for (Point point : originalRegion.accessiblePoints) {
                // 初始化最近泊位和路径长度
                Berth closestBerth = null;
                List<Point> shortestPath = null;

                for (Berth berth : originalRegion.getBerths()) {
                    List<Point> path = berth.mapPath.get(point);
                    if (path != null && (shortestPath == null || path.size() < shortestPath.size())) {
                        closestBerth = berth;
                        shortestPath = path;
                    }
                }
                closestBerth.points += 1;
                // 更新离点最近的泊位信息
                pointBerthMap.put(point, closestBerth);
            }
        }
    }

    /**
     * @function 对初始的region进一步划分，通过泊位之间的距离计算出阈值，作为聚类的条件
     */
    private void splitRegions() {
        List<Region> newRegions = new ArrayList<>();
        for (Region largeRegion : regions) {
            // 获取泊位对应的所有点
            Set<Point> points = largeRegion.getBerths().stream().map(berth -> berth.pos).collect(Collectors.toSet());

            // 初始化并查集
            UnionFind unionFind = new UnionFind(points);

            // 计算泊位间的距离并确定阈值
            int threshold = calculateThreshold(largeRegion);

            // 创建新的 Zone 对象并添加区域相关信息
            Zone zone = new Zone(zones.size());
            zone.accessPoints.addAll(largeRegion.getAccessiblePoints());
            zone.berths.addAll(largeRegion.getBerths());

            // 基于阈值和berth拥有的点综合对泊位进行合并
            mergeBerths2NewRegions(largeRegion, threshold, unionFind, newRegions, zone);
            zones.add(zone);  // 将 Zone 添加到全局列表

            // 将大区域内剩余的点分配到最近的新区域
            allocateRemainingPoints(largeRegion);

            // 为新分割的 region 添加邻近的region
            addNeighborRegion(newRegions);
        }

        // 更新区域集合
        regions.clear();
        regions.addAll(newRegions);
    }

    // 根据不同泊位之间的距离，计算出合适的阈值
    private int calculateThreshold(Region region) {
        if (region.getBerths().isEmpty()) {
            return unreachableFps;  // 没有泊位时返回不可达值
        }

        List<Integer> distances = new ArrayList<>();
        ArrayList<Berth> berthsList = new ArrayList<>(region.getBerths());
        for (int i = 0; i < berthsList.size(); i++) {
            for (int j = i + 1; j < berthsList.size(); j++) {
                Berth berth1 = berthsList.get(i);
                Berth berth2 = berthsList.get(j);
                int distance = berth1.mapPath.get( berth2.pos).size();
                if (distance < unreachableFps) {
                    distances.add(distance);
                }
            }
        }

        if (distances.isEmpty()) {
            return unreachableFps;
        }
        Collections.sort(distances);
        int thresholdIndex = (int) (distances.size() * upperQuantile);  // 调整百分比以得到所需阈值大小
        return Math.min(distances.get(thresholdIndex), maxThreshold);
    }

    // 将根据阈值划分的泊位进行聚合，定义新的region
    private void mergeBerths2NewRegions(Region largeRegion, int threshold, UnionFind unionFind, List<Region> newRegions, Zone zone) {
        ArrayList<Berth> berthsList = new ArrayList<>(largeRegion.getBerths());

        // 首先根据距离进行聚类
        initialMergeByDistance(berthsList, unionFind, threshold);

        // 然后按照每个根节点下的泊位点数总和进行进一步处理
        Map<Point, Integer> rootPointsSum = calculateRootPointsSum(berthsList, unionFind);

        // 创建区域并分配泊位
        Map<Point, Region> rootToRegion = new HashMap<>();
        Set<Berth> unassignedBerths = new HashSet<>(berthsList);
        secondMergeByPointsCounts(berthsList, unionFind, newRegions, zone, rootPointsSum, rootToRegion, unassignedBerths);

        // 处理未分配的泊位
        processUnassignedBerths(unassignedBerths, berthsList, unionFind, rootToRegion, newRegions, zone);
    }

    // 按照距离进行第一次聚类
    private void initialMergeByDistance(ArrayList<Berth> berthsList, UnionFind unionFind, int threshold) {
        for (int i = 0; i < berthsList.size(); i++) {
            for (int j = i + 1; j < berthsList.size(); j++) {
                Berth berth1 = berthsList.get(i);
                Berth berth2 = berthsList.get(j);
                List<Point> path = berth1.mapPath.get(berth2.pos);
                if (path != null && path.size() <= threshold) {
                    unionFind.union(berth1.pos, berth2.pos);
                }
            }
        }
    }

    // 计算各个泊位聚类拥有的点数
    private Map<Point, Integer> calculateRootPointsSum(ArrayList<Berth> berthsList, UnionFind unionFind) {
        Map<Point, Integer> rootPointsSum = new HashMap<>();
        for (Berth berth : berthsList) {
            Point root = unionFind.find(berth.pos);
            rootPointsSum.merge(root, berth.points, Integer::sum);
        }
        return rootPointsSum;
    }

    // 对已经聚好类的泊位进行创建新区域
    private void secondMergeByPointsCounts(ArrayList<Berth> berthsList, UnionFind unionFind, List<Region> newRegions, Zone zone, Map<Point, Integer> rootPointsSum, Map<Point, Region> rootToRegion, Set<Berth> unassignedBerths) {
        for (Point root : rootPointsSum.keySet()) {
            // 如果这个聚类的总点数足以形成一个区域
            if (rootPointsSum.get(root) >= minPointsPercent * pointSet.size()) {
                Region region = new Region(newRegions.size());
                newRegions.add(region);
                zone.addRegion(region);
                rootToRegion.put(root, region);

                // 遍历所有泊位，将属于这个根节点的泊位加入新区域，并从未分配泊位中移除
                for (Berth berth : berthsList) {
                    if (unionFind.find(berth.pos).equals(root)) {
                        region.addBerth(berth);
                        pointRegionMap.put(berth.pos, region);
                        unassignedBerths.remove(berth);
                    }
                }
            }
        }
    }

    private void processUnassignedBerths(Set<Berth> unassignedBerths, ArrayList<Berth> berthsList, UnionFind unionFind, Map<Point, Region> rootToRegion, List<Region> newRegions, Zone zone) {
        Set<Berth> assignedBerths = new HashSet<>();

        for (Berth unassignedBerth : unassignedBerths) {
            // 寻找最近的berth
            Berth nearestBerth = findNearestBerth(unassignedBerth, berthsList, unionFind, rootToRegion);

            int combinedPoints = unassignedBerth.points + nearestBerth.points; // 将两个berth的points相加
            // 如果最近距离的 berth 不存在region，且未处理，同时符合合并条件，那么创建新区域
             if (!rootToRegion.containsKey(unionFind.find(nearestBerth.pos)) && !assignedBerths.contains(nearestBerth) && combinedPoints >= minPointsPercent * pointSet.size()) {
                Region newRegion = new Region(newRegions.size());
                newRegions.add(newRegion);
                zone.addRegion(newRegion);
                newRegion.addBerth(unassignedBerth);
                newRegion.addBerth(nearestBerth);
                pointRegionMap.put(unassignedBerth.pos, newRegion);
                pointRegionMap.put(nearestBerth.pos, newRegion);
                rootToRegion.put(unionFind.find(unassignedBerth.pos), newRegion);
                rootToRegion.put(unionFind.find(nearestBerth.pos), newRegion);
                assignedBerths.add(nearestBerth);
            } else {
                Region closestRegion = findClosestRegion(unassignedBerth, newRegions);
                closestRegion.addBerth(unassignedBerth);
                pointRegionMap.put(unassignedBerth.pos, closestRegion);
                rootToRegion.put(unionFind.find(unassignedBerth.pos), closestRegion);
            }
            assignedBerths.add(unassignedBerth);
        }
    }

    // 查找给定泊位最近的泊位
    private Berth findNearestBerth(Berth targetBerth, ArrayList<Berth> berthsList, UnionFind unionFind, Map<Point, Region> rootToRegion) {
        Berth nearestBerth = null;
        int minDistance = unreachableFps;
        for (Berth berth : berthsList) {
            if (!berth.equals(targetBerth)) {
                int distance = targetBerth.mapPath.get(berth.pos).size();
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestBerth = berth;
                }
            }
        }
        return nearestBerth;
    }

    // 查找给定泊位最近的区域
    private Region findClosestRegion(Berth berth, List<Region> regions) {
        Region closestRegion = null;
        int shortestDistance = unreachableFps;
        for (Region region : regions) {
            for (Berth otherBerth : region.getBerths()) {
                int distance = berth.mapPath.get(otherBerth.pos) != null ? berth.mapPath.get(otherBerth.pos).size() : Integer.MAX_VALUE;
                if (distance < shortestDistance) {
                    shortestDistance = distance;
                    closestRegion = region;
                }
            }
        }
        return closestRegion;
    }

    /**
     * @function 将剩余点进行分配，根据点离得最近的泊位作为区域划分的条件
     */
    private void allocateRemainingPoints(Region largeRegion) {
        for (Point point : largeRegion.accessiblePoints) {
            // 从 globalPointToClosestBerth 映射中获取最近的泊位
            Berth closestBerth = pointBerthMap.get(point);
            // 找到这个泊位所在的区域
            Region closestRegion = pointRegionMap.get(closestBerth.pos);

            // 确保找到了最近泊位和所属区域
            // 将点加入到最近的区域
            closestRegion.addAccessiblePoint(point);

            // 更新点到区域的映射
            pointRegionMap.put(point, closestRegion);

            // 获取点到最近泊位的路径长度
            List<Point> shortestPath = closestBerth.mapPath.get(point);
            // 更新路径长度的统计信息
            if (shortestPath != null) {
                int pathLength = shortestPath.size() - 1; // 减1以排除起始点本身
                closestRegion.pathLenToNumMap.merge(pathLength, 1, Integer::sum);
                closestBerth.pathLenToNumMap.merge(pathLength, 1, Integer::sum);
            }

        }
    }

    /**
     * @function 为 region 添加 neighborRegion，
     */
    // 为新分割的 region 增加邻居 region
    private void addNeighborRegion(List<Region> newRegions) {
        for (Region currentRegion : newRegions) {
            Map<Region, Integer> neighborDistances = new HashMap<>();

            for (Berth currentBerth : currentRegion.getBerths()) {
                for (Region potentialNeighborRegion : newRegions) {
                    if (currentRegion == potentialNeighborRegion) {
                        continue;
                    }

                    for (Berth neighborBerth : potentialNeighborRegion.getBerths()) {
                        List<Point> path = currentBerth.mapPath.get(neighborBerth.pos);
                        if (path != null) {
                            int currentDistance = path.size();
                            neighborDistances.merge(potentialNeighborRegion, currentDistance, Math::min);
                        }
                    }
                }
            }

            List<Map.Entry<Region, Integer>> sortedNeighbors = new ArrayList<>(neighborDistances.entrySet());
            sortedNeighbors.sort(Map.Entry.comparingByValue());

            currentRegion.neighborRegions.clear();
            for (Map.Entry<Region, Integer> entry : sortedNeighbors) {
                currentRegion.neighborRegions.add(entry.getKey());
            }
        }
    }

    /**
     * @function 为zone分配机器人，
     */
    private void assignRobotsToZone() {
        // 遍历每个机器人
        for (Robot robot : robots) {  // 直接使用 robots，无需 Const.robots
            // 获取机器人的当前位置
            Point robotPosition = robot.pos;
            if (robotPosition == null) {
                printLog("Robot position is null!");
                continue;
            }

            // 判断该机器人属于哪个zone
            for (Zone zone : zones) {
                // 如果机器人的当前位置是zone中的一个可达点
                if (zone.accessPoints.contains(robotPosition)) {
                    // 将机器人添加到该zone的机器人集合中
                    zone.robots.add(robot);
                    break; // 找到所属zone后不需要继续判断其他zone
                }
            }
        }
    }

    /**
     * 给每个区域静态划分机器人，保证每个区域至少一个机器人，机器人少于区域数另说
     */
    private void assignRobotsToRegion() {
        // 不行的话按照距离前80个点为准
        for (Region region : regions) {
            region.calcStaticValue();
        }
        //计算每个区域应该分多少机器人
        for (Zone zone : zones) {
            zone.assignRegionRobotNum();
        }
//        printRegion();
        // 给每个区域具体划分机器人，看其位置，就近划分
        for (Zone zone : zones) {
            zone.assignSpecificRegionRobot();
        }
    }

    /**
     * @function 计算region的新增货物，
     */
    public void addNewGood(Good newGood) {
        if (newGood == null) {
            Util.printErr("addNewGood newGood == null");
            return;
        }
        // 增加新物品
        Region region = pointRegionMap.get(newGood.pos);
        if (region == null) {
            Util.printErr("addNewGood region == null");
            return;
        }

        region.addNewGood(newGood);
        printDebug("打印新增物品信息");
        printLog(region);
        printLog(region.regionGoodsByTime);
        printLog(region.regionGoodsByValue);
    }

    public void printAll() {
        //        for (Zone zone : zones) {
//            printLog(zone);
//        }
//        for (Region region : regions) {
//            if (region.id == 0 || region.id == 8){
//                Util.printDebug("查看价值："+region +":size："+region.accessiblePoints.size()+region.berths);
//                Util.printLog("pathLenToNumMap"+region.pathLenToNumMap);
//                Util.printLog(region.staticValue);
//                Util.printLog("---");
//            }
//        }

        for (Region region : regions) {
            Util.printDebug("查看价值：" + region + ":size：" + region.accessiblePoints.size() + region.berths + "机器人数：" + region.assignedRobots.size());
            Util.printLog(region.staticValue.get(1));
            Util.printLog(region.staticValue.get(2));
            Util.printLog(region.staticValue.get(3));
            Util.printLog("---");
        }

//
//        Util.printLog("打印区域分配信息");
//        Util.printDebug(pointRegionMap);
//        for (Map.Entry<Point, Region> pointRegionEntry : pointRegionMap.entrySet()) {
//            printLog(pointRegionEntry.getKey()+"->"+pointRegionEntry.getValue().getId());
//        }
//        for (Map.Entry<Point, Map<Berth, List<Point>>> pointMapEntry : globalPointToClosestBerth.entrySet()) {
//            Util.printDebug(pointMapEntry.getKey());
//            for (Map.Entry<Berth, List<Point>> entry : pointMapEntry.getValue().entrySet()) {
//                Util.printDebug(entry.getKey());
//            }
//        }


//        printRegion();
    }

    void printRegion() {
        for (Region region : regions) {
            printLog(region);
            printLog(region.staticAssignNum);
            printLog(region.assignedRobots);
        }
    }
}
