package com.huawei.codecraft.zone;

import com.huawei.codecraft.Const;
import com.huawei.codecraft.Util;
import com.huawei.codecraft.core.Berth;
import com.huawei.codecraft.core.DeliveryPoint;
import com.huawei.codecraft.core.Good;
import com.huawei.codecraft.core.Robot;
import com.huawei.codecraft.util.Point;
import com.huawei.codecraft.util.RegionValue;
import com.huawei.codecraft.util.UnionFind;
import com.huawei.codecraft.way.Mapinfo;

import java.util.*;
import java.util.stream.Collectors;

import static com.huawei.codecraft.Const.*;
import static com.huawei.codecraft.Util.printLog;
import static com.huawei.codecraft.way.Mapinfo.*;

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
    private static final int[][][] berthId = {
            {{0,1},{2, 3},{4, 5},{6},{7, 8}, {9}},
            {{0, 2}, {1}, {3, 4}, {5}, {6}, {7}, {8, 9}},
    };
    /**
     * 构造函数
     */
    public RegionManager() {

    }

    public void init() {
        createInitialRegions();
        getFullPathsFromPoints2Berths();
        initGlobalPoint2ClosestBerthMap();
        initRectangleArea();
        splitRegions();
        allocateBerthingPoints(); // 分配泊位得靠泊点
        calcRegionValue(); // 给区域分配机器人
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

    public void testAllocateBerthingPoints() {
        allocateBerthingPoints(); // 分配泊位得靠泊点
    }

    public void testSplitRegions() {
        splitRegions();
    }

    public void testInitRectangleArea() {
        initRectangleArea();
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
            if (!isValid(x, y) || Mapinfo.map[x][y] < MAINBOTH || pointSet.contains(current)) {
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
            System.out.println("threshold:" + threshold);
            // 创建新的 Zone 对象并添加区域相关信息
            Zone zone = new Zone(zones.size());
            zone.accessPoints.addAll(largeRegion.getAccessiblePoints());
            zone.berths.addAll(largeRegion.getBerths());

            // 基于阈值和berth拥有的点综合对泊位进行合并
            if (mapSeq == defaultMap) {
                mergeBerths2NewRegions(largeRegion, threshold, unionFind, newRegions, zone);
            }
            else {
                mergeBerthByDefaultParam(newRegions, zone);
            }
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

    // 手动划分区域接口
    private void mergeBerthByDefaultParam(List<Region> newRegions, Zone zone) {
        int[][] mergeBerthId = berthId[mapSeq - 1];
        for (int[] row : mergeBerthId) {
            Region region = new Region(newRegions.size());
            newRegions.add(region);
            zone.addRegion(region);
            for (int value : row) {
                Berth  berth = idToBerth.get(value);
                region.addBerth(berth);
                pointRegionMap.put(berth.pos, region);
            }
        }
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
            if (rootPointsSum.get(root) > minPointsPercent * pointSet.size()) {
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
            if (nearestBerth == null) { // 最近泊位为空，则证明该泊位是一个单独的区域
                Region newRegion = new Region(newRegions.size());
                newRegions.add(newRegion);
                zone.addRegion(newRegion);
                newRegion.addBerth(unassignedBerth);
                pointRegionMap.put(unassignedBerth.pos, newRegion);
                assignedBerths.add(unassignedBerth);
                continue;
            }
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
            if (shortestPath != null ) {
                // 泊口不会产生货物，不算进去
                int pathLength = shortestPath.size() - 1; // 减1以排除起始点本身
                closestRegion.pathLenToNumMap.merge(pathLength, 1, Integer::sum);
                closestBerth.pathLenToNumMap.merge(pathLength, 1, Integer::sum);
            }
        }
        // 泊口不产生物品，这些点要去除
        for (Region region : regions) {
            region.pathLenToNumMap.merge(1, -3, Integer::sum);
            region.pathLenToNumMap.merge(2, -2, Integer::sum);
        }
        for (Berth berth : berths) {
            berth.pathLenToNumMap.merge(1, -3, Integer::sum);
            berth.pathLenToNumMap.merge(2, -2, Integer::sum);
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

    private void allocateBerthingPoints() {
        for (int i = 0; i < Const.mapWidth; i++) {
            for (int j = 0; j < Const.mapWidth; j++) {
                // 如果该点是泊位点，加入到对应得泊位停靠表中
                if (originalMap[i][j] == 'K' || originalMap[i][j] == 'B') {
                    int minDistance = unreachableFps;
                    Berth ClosestBerth = null;
                    for (Berth berth : berths) {
                        int distance = Math.abs(berth.pos.x - i) + Math.abs(berth.pos.y - j);
                        if (distance < minDistance) {
                            minDistance = distance;
                            ClosestBerth = berth;
                        }
                    }
                    assert ClosestBerth != null;
                    ClosestBerth.boatInBerthArea.add(new Point(i, j));
                }
            }
        }
    }

    private void initRectangleArea() {
        initBerthRectangleArea();
        initDeliveryPointRectangleArea();
    }

    private void initBerthRectangleArea() {
        for (Berth berth : berths) {
            Point core = berth.pos;
            HashSet<Point> pointSet;

            // 获得水平方向上的矩形
            pointSet = getRectanglePointsVertical(core);
            berth.rectangleAreaPoints.add(pointSet);

            // 获得垂直方向的矩形
            pointSet = getRectanglePointsHorizon(core);
            berth.rectangleAreaPoints.add(pointSet);
        }
    }

    private void initDeliveryPointRectangleArea() {
        for (DeliveryPoint deliveryPoint : deliveryPoints) {
            HashSet<Point> pointSet;
            Point core = deliveryPoint.pos;

            // 获得水平方向上的矩形
            pointSet = getRectanglePointsVertical(core);
            deliveryPoint.deliveryRectangleAreaPoints.add(pointSet);

            // 获得垂直方向的矩形
            pointSet = getRectanglePointsHorizon(core);
            deliveryPoint.deliveryRectangleAreaPoints.add(pointSet);
        }
    }

    private  HashSet<Point> getRectanglePointsVertical(Point core) {
        // 垂直方向扩散得到区域
        // 水平方向增长
        int up = -1;
        int down = 1;
        int upLen = unreachableFps;
        int downLen = unreachableFps;
        Point core1 = new Point(core.x, core.y);
        Point core2 = new Point(core.x, core.y);

        // 从core开始计算每个的最大上宽和最大下宽
        while (isValid(core1.x, core1.y) && seaMap[core1.x][core1.y] != Const.ROAD) {
            Point upPoint = new Point(core1.x + up, core1.y);
            Point downPoint = new Point(core1.x + down, core1.y);
            upLen = Math.min(upLen, getVerticalLen(upPoint, core1, up));
            downLen = Math.min(downLen, getVerticalLen(downPoint, core1, down));
            core1.y -= 1;
        }
        // 求出右边的宽度
        while (isValid(core2.x, core2.y) && seaMap[core2.x][core2.y] != Const.ROAD) {
            Point upPoint = new Point(core2.x + up, core2.y);
            Point downPoint = new Point(core2.x + down, core2.y);
            upLen = Math.min(upLen, getVerticalLen(upPoint, core2, up));
            downLen = Math.min(downLen, getVerticalLen(downPoint, core2, down));
            core2.y += 1;
        }
        int leftLen = Math.abs(core1.y - core.y) - 1;
        int rightLen = Math.abs(core2.y - core.y) - 1;

        // 将整个矩形中的点加入到hashset
        HashSet<Point> pointSet = new HashSet<>();
        for (int i = core.x - upLen; i <= core.x + downLen; i++) {
            for (int j = core.y - leftLen; j <= core.y + rightLen; j++) {
                if (seaMap[i][j] != Const.ROAD) {
                    pointSet.add(new Point(i, j));
                }
            }
        }
        return pointSet;
    }

    private  HashSet<Point> getRectanglePointsHorizon(Point core) {
        // 水平方向扩散
        // 垂直方向增长的区域
        int left = -1;
        int right = 1;
        int leftLen = unreachableFps;
        int rightLen = unreachableFps;
        Point core1 = new Point(core.x, core.y);
        Point core2 = new Point(core.x, core.y);

        // 从core开始计算每个的最大上宽和最大下宽
        while (isValid(core1.x, core1.y) && seaMap[core1.x][core1.y] != Const.ROAD) {
            Point leftPoint = new Point(core1.x, core1.y  + left);
            Point rightPoint = new Point(core1.x + right, core1.y + right);
            leftLen = Math.min(leftLen, getHorizonLen(leftPoint, core1, left));
            rightLen = Math.min(rightLen, getHorizonLen(rightPoint, core1, right));
            core1.x -= 1;
        }
        // 求出右边的宽度
        while (isValid(core2.x, core2.y) && seaMap[core2.x][core2.y] != Const.ROAD) {
            Point leftPoint = new Point(core2.x , core2.y + left);
            Point rightPoint = new Point(core2.x, core2.y + right);
            leftLen = Math.min(leftLen, getHorizonLen(leftPoint, core2, left));
            rightLen = Math.min(rightLen, getHorizonLen(rightPoint, core2, right));
            core2.x += 1;
        }
        int upLen = Math.abs(core1.x - core.x) - 1;
        int downLen = Math.abs(core2.x - core.x) - 1;;

        // 将整个矩形中的点加入到hashset
        HashSet<Point> pointSet = new HashSet<>();
        for (int i = core.x - upLen; i <= core.x + downLen; i++) {
            for (int j = core.y - leftLen; j <= core.y + rightLen; j++) {
                if (seaMap[i][j] != Const.ROAD) {
                    pointSet.add(new Point(i, j));
                }
            }
        }
        return pointSet;
    }

    private int getVerticalLen(Point point, Point core, int direction) {
        while (isValid(point.x, point.y) && seaMap[point.x][point.y] != Const.ROAD) {
            point.x += direction;
        }
        return Math.abs(point.x - core.x) - 1;
    }

    private int getHorizonLen(Point point, Point core, int direction) {
        while (isValid(point.x, point.y) && seaMap[point.x][point.y] != Const.ROAD) {
            point.y += direction;
        }
        return Math.abs(point.y - core.y) - 1;
    }

    /**
     * 计算区域与泊口的静态价值
     */
    private void calcRegionValue() {
        for (Berth berth : berths) {
            int count=0;
            for (Integer i : berth.pathLenToNumMap.keySet()) {
                count +=berth.pathLenToNumMap.get(i);
            }
            Util.printLog(berth.pathLenToNumMap);
            berth.staticValue = calcStaticValue(berth.pathLenToNumMap,berth.points);
        }

        for (Region region : regions) {
            Util.printLog(region.pathLenToNumMap);
            region.staticValue = calcStaticValue(region.pathLenToNumMap,region.accessiblePoints.size());
        }

//        printRegion();
        // 给每个区域具体划分机器人，看其位置，就近划分
    }

    public static Map<Integer, RegionValue> calcStaticValue(Map<Integer,Integer> pathLenToNumMap, int area) {

        // 计算机器人的静态价值
        // 第一优先级：面积够的 > 面积不够的；第二优先级，平均距离少的 > 平均距离远的
        Map<Integer, RegionValue> staticValue = new HashMap<>();
        double dis = 0;   // t为理想机器人搬运货物走的总fps
        double p = getPointProb()/totalFrame * Good.maxSurvive;     // 每点全局概率：0.125左右，周期内每个点产生的概率0.008;
        int total = Good.maxSurvive;   //往返fps，只有一半的时间是在去的路上
        int robotNum = 1;
        double totalNum = 0;
//        Util.printLog("周期概率"+p);
//        Util.printLog("pathLenToNumMap"+pathLenToNumMap);
        for (int i = 1; i < 1000; i++) {
            if (pathLenToNumMap.containsKey(i)){
                int num = pathLenToNumMap.get(i);
                double realNum= num * p;
                dis += i * realNum * 2;     //往返fps，只有一半的时间是在去的路上
                totalNum += realNum;
                if (dis > total){ // 时间到了，不能在运
                    double more = (dis - total)/2/i;  //加多了，减回去几个
                    totalNum -= more;  //加多了，减回去几个
//                    Util.printLog("robotNum"+robotNum+",index="+i+",dis"+dis+",num="+num + ",more="+more/p);
                    staticValue.put(robotNum,new RegionValue(robotNum,true,i,totalNum, (int) ((realNum-more)/p)));
                    robotNum ++;
                    total += Good.maxSurvive; // 2个机器人搬运距离翻倍
                }
            }else {
                staticValue.put(robotNum,new RegionValue(robotNum,false,i, area * p,0));
                break;
            }
        }
        return staticValue;
    }

    public static double getPointProb() {
        // 计算每个点生成的概率
        // 计算所有空地面积
//        int area = 1;
//        for (Zone zone : zones) {
//            area +=zone.accessPoints.size();
//        }
//        // 每个点的期望 = 所有物品 / 总点数
//        Util.printLog("可用区域点数："+area);
//        Util.printLog("实际区域点数："+totalLandPoint);
//        Util.printLog("每点概率："+expGoodNum / area);
        return expGoodNum / totalLandPoint;
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
            Util.printWarn("addNewGood region == null");
            return;
        }

        region.addNewGood(newGood);
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
