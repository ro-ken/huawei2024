package com.huawei.codecraft.zone;

import com.huawei.codecraft.core.Berth;
import com.huawei.codecraft.core.Robot;
import com.huawei.codecraft.util.Point;
import com.huawei.codecraft.way.Path;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static com.huawei.codecraft.Const.*;
import static com.huawei.codecraft.Util.printLog;
import static com.huawei.codecraft.way.Mapinfo.isValid;
import static com.huawei.codecraft.way.Mapinfo.map;

/**
 * ClassName: RegionManager
 * Package: com.huawei.codecraft.way
 * Description: 管理region，向上提供接口
 */
public class RegionManager {
    public final List<Region> regions;     // 地图所有的region
    private final Map<Point, Region> pointRegionMap;    // point 到 region 的映射，查找点属于的 region
    private final Map<Point, Map<Berth, List<Point>>> globalPointToClosestBerthPath;    // 获取离点最近的泊位的路径

    private final Path pathFinder;  // Path 接口的引用

    /**
     * 构造函数
     *
     */
    public RegionManager(Path pathFinder) {
        this.regions = new ArrayList<>();
        this.pointRegionMap = new HashMap<>();
        this.pathFinder = pathFinder;  // 通过构造器注入 Path 实现
        this.globalPointToClosestBerthPath = new HashMap<>();
        createRegions();
        splitRegions();
        assignRobots();
        for (Zone zone : zones) {
            printLog(zone);
        }

    }

    /**
     * 接口函数定义位置，向上提供接口
     *
     */
    public Region getRegionByPoint(Point point) {
        return pointRegionMap.get(point);
    }

    /**
     * @function 创建初始的连通区域，根据地图的联通性划分
     *
     */
    public void createRegions() {
        for (Berth berth : berths) {
            Point berthPoint = berth.pos;
            if (!pointRegionMap.containsKey(berthPoint)) {
                Region region = new Region(regions.size());
                exploreRegion(berthPoint.x, berthPoint.y, region);
                if (!region.getAccessiblePoints().isEmpty()) {
                    regions.add(region);
                }
            }
        }
    }

    // 开始探索地图，获取最初的region
    private void exploreRegion(int startX, int startY, Region region) {
        Queue<Point> queue = new LinkedList<>();
        queue.add(new Point(startX, startY));

        while (!queue.isEmpty()) {
            Point current = queue.poll();
            int x = current.x;
            int y = current.y;

            // 跳过无效点或已经探索的点
            if (!isValid(x, y) || map[x][y] != 0 || pointRegionMap.containsKey(current)) {
                continue;
            }

            // 标记点为已探索，添加到区域中
            pointRegionMap.put(current, region);
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
     * ClassName: RegionManager
     * Package: com.huawei.codecraft.way
     * Description: 为berth进行聚类的时候，根据并查集，使集合唯一
     */
    public static class UnionFind {
        private final Map<Point, Point> parent;

        public UnionFind(Set<Point> points) {
            parent = new HashMap<>();
            for (Point point : points) {
                parent.put(point, point);
            }
        }

        public Point find(Point point) {
            Point p = point;
            while (!p.equals(parent.get(p))) {
                p = parent.get(p);
            }
            // Path compression
            Point root = p;
            p = point;
            while (!p.equals(root)) {
                Point next = parent.get(p);
                parent.put(p, root);
                p = next;
            }
            return root;
        }

        public void union(Point point1, Point point2) {
            Point root1 = find(point1);
            Point root2 = find(point2);
            if (!root1.equals(root2)) {
                parent.put(root1, root2);
            }
        }
    }

    /**
     * @function 对初始的region进一步划分，通过泊位之间的距离计算出阈值，作为聚类的条件
     *
     */
    public void splitRegions() {
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

            // 基于阈值合并泊位到新区域
            mergeBerths2NewRegions(largeRegion, threshold, unionFind, newRegions, zone);
            zones.add(zone);  // 将 Zone 添加到全局列表

            // 将大区域内剩余的点分配到最近的新区域
            allocateRemainingPoints(largeRegion, newRegions);

            // 为新分割的 region 添加邻近的region
            addNeighborRegion(newRegions);
        }

        // 更新区域集合
        this.regions.clear();
        this.regions.addAll(newRegions);
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
                int distance = pathFinder.getPathFps(berth1.pos, berth2.pos);
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
        return Math.min(distances.get(thresholdIndex), minThreshold);
    }


    // 将根据阈值划分的泊位进行聚合，定义新的region
    private void mergeBerths2NewRegions(Region largeRegion, int threshold, UnionFind unionFind, List<Region> newRegions, Zone zone) {
        ArrayList<Berth> berthsList = new ArrayList<>(largeRegion.getBerths());
        for (int i = 0; i < berthsList.size(); i++) {
            for (int j = i + 1; j < berthsList.size(); j++) {
                Berth berth1 = berthsList.get(i);
                Berth berth2 = berthsList.get(j);
                Point point1 = berth1.pos;
                Point point2 = berth2.pos;
                int distance = pathFinder.getPathFps(point1, point2);
                if (distance <= threshold) {
                    unionFind.union(point1, point2);
                }
            }
        }

        Map<Point, Region> rootToRegion = new HashMap<>();
        for (Point point : unionFind.parent.keySet()) {
            Point root = unionFind.find(point);
            if (!rootToRegion.containsKey(root)) {
                Region region = new Region(newRegions.size());
                newRegions.add(region);  // 先添加，确保ID的唯一性
                rootToRegion.put(root, region);
                zone.regions.add(region);  // 将新区域添加到Zone的regions集合中
            }
            Region region = rootToRegion.get(root);
            Berth berth = pointToBerth.get(point);
            if (berth != null && !region.getBerths().contains(berth)) {
                region.addBerth(berth);
            }
        }
    }

    // 为新分割的 region 增加邻居 region
    private void addNeighborRegion(List<Region> newRegions) {
        for (Region currentRegion : newRegions) {
            Map<Region, Integer> neighborDistances = new HashMap<>(); // 应该在这里初始化，保证每个区域的邻居信息是独立的

            for (Berth currentBerth : currentRegion.getBerths()) {
                for (Region potentialNeighborRegion : newRegions) {
                    if (currentRegion == potentialNeighborRegion) {
                        continue; // Skip self
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

            currentRegion.neighborRegions.clear(); // Ensure neighborRegions is empty before adding new neighbors
            for (Map.Entry<Region, Integer> entry : sortedNeighbors) {
                currentRegion.neighborRegions.add(entry.getKey());
            }
        }
    }


    // 将剩余点进行分配，根据点离得最近的泊位作为区域划分的条件
    private void allocateRemainingPoints(Region largeRegion, List<Region> newRegions) {
//        Map<Point, Region> tempPointRegionMap = new HashMap<>();
//        addBerthArea(tempPointRegionMap, newRegions);
        for (Berth berth : largeRegion.getBerths()) {
            bfsFromBerth(berth);
        }

        // 根据bfs得到点到泊位的最短路进行划分点的所属区域
        allocatepoint2region(newRegions);
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

                if (pointRegionMap.containsKey(nextPoint) && !visited[newX][newY]) {
                    visited[newX][newY] = true;

                    List<Point> newPath = new ArrayList<>(currentPath);
                    newPath.add(nextPoint);

                    visitedPaths.put(nextPoint, newPath);
                    queue.add(nextPoint);  // 将新探索点加入队列

                    // 更新全局最短路径信息
                    globalPointToClosestBerthPath.compute(nextPoint, (k, existingPaths) -> {
                        if (existingPaths == null) {
                            existingPaths = new HashMap<>();
                        }
                        List<Point> existingPath = existingPaths.get(berth);
                        if (existingPath == null || newPath.size() < existingPath.size()) {
                            existingPaths.put(berth, newPath);
                        }
                        return existingPaths;
                    });
                }
            }
        }

        // 将每个泊位的路径信息更新到泊位对象
        for (Map.Entry<Point, List<Point>> entry : visitedPaths.entrySet()) {
            List<Point> path = entry.getValue();
            Point endPoint = path.get(path.size() - 1);

            // 确保路径以泊位为起点，或者路径终点为泊位需要反转
            if (!path.isEmpty() && (path.get(0).equals(start) || endPoint.equals(start))) {
                if (endPoint.equals(start)) {
                    // 如果路径的终点是泊位，则反转路径
                    Collections.reverse(path);
                    // 反转后，起点应该是泊位，我们将反转后的路径存储
                    berth.mapPath.put(entry.getKey(), path);
                } else {
                    // 路径以泊位为起点，直接存储
                    berth.mapPath.put(entry.getKey(), path);
                }
            }
        }

        // 保证泊位点归属于泊位的所在区域
        globalPointToClosestBerthPath.computeIfAbsent(start, k -> new HashMap<>()).put(berth, new ArrayList<>(Collections.singletonList(start)));
    }


    // 将点分配给其所属的区域，按照距离进行划分
    private void allocatepoint2region(List<Region> newRegions) {
        for (Map.Entry<Point, Map<Berth, List<Point>>> entry : globalPointToClosestBerthPath.entrySet()) {
            Point point = entry.getKey();
            Map<Berth, List<Point>> closestPaths = entry.getValue();

            Berth closestBerth = null;
            List<Point> shortestPath = null;

            for (Map.Entry<Berth, List<Point>> berthEntry : closestPaths.entrySet()) {
                if (shortestPath == null || berthEntry.getValue().size() < shortestPath.size()) {
                    closestBerth = berthEntry.getKey();
                    shortestPath = berthEntry.getValue();
                }
            }

            if (closestBerth != null) {
                Region region = findRegionByBerth(closestBerth, newRegions);
                if (region != null) {
                    region.addAccessiblePoint(point);
                }
            }
        }
    }

    // 根据泊位找到对应的区域
    private Region findRegionByBerth(Berth berth, List<Region> newRegions) {
        for (Region region : newRegions) {
            if (region.getBerths().contains(berth)) {
                return region;
            }
        }
        return null;
    }

    /**
     * @function 为zone分配机器人，
     *
     */
    private void assignRobots() {
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

    /**********************************************************************************
     * 暂留接口函数，后续看测试是否需要，后续整理在删除
     * @function getFullPath: 获取所有的点到泊位，泊位到点的路径
     * @function bfsFromBerthForFullPaths: bfs遍历，在getFullPath中被调用
     * @function addBerthArea: 确定berth的区域，暂时好像没用
     * *********************************************************************************
     */
    public void getFullPath() {
        // 为每个泊位计算到所有其他点的路径
        for (Berth berth : berths) {
            Map<Point, List<Point>> berthPaths = new HashMap<>();
            bfsFromBerthForFullPaths(berth, berthPaths);
//            globalBerthToPointPaths.put(berth, berthPaths);

            // 根据计算出的泊位到点的路径构建点到泊位的路径
            for (Map.Entry<Point, List<Point>> entry : berthPaths.entrySet()) {
                Point point = entry.getKey();
                List<Point> path = entry.getValue();
                List<Point> reversedPath = new ArrayList<>(path);
                Collections.reverse(reversedPath); // 反转路径

//                globalPointToBerthPaths.computeIfAbsent(point, k -> new HashMap<>());
//                globalPointToBerthPaths.get(point).put(berth, reversedPath);
            }
        }
    }

    private void bfsFromBerthForFullPaths(Berth berth, Map<Point, List<Point>> targetMap) {
        Queue<Point> queue = new LinkedList<>();
        Map<Point, List<Point>> visitedPaths = new HashMap<>();
        Point start = berth.pos;

        queue.add(start);
        visitedPaths.put(start, new ArrayList<>(Collections.singletonList(start)));

        while (!queue.isEmpty()) {
            Point current = queue.poll();
            List<Point> currentPath = visitedPaths.get(current);

            for (int[] dir : new int[][]{{0, 1}, {0, -1}, {-1, 0}, {1, 0}}) {
                int newX = current.x + dir[0];
                int newY = current.y + dir[1];
                Point nextPoint = new Point(newX, newY);

                if (pointRegionMap.containsKey(nextPoint) && !visitedPaths.containsKey(nextPoint)) {
                    List<Point> newPath = new ArrayList<>(currentPath);
                    newPath.add(nextPoint);
                    visitedPaths.put(nextPoint, newPath);
                    queue.add(nextPoint);
                }
            }
        }

        // 保存从泊口到所有点的路径
        targetMap.putAll(visitedPaths);
    }

    // 暂留函数，待后续再看是否会用
    private void addBerthArea(Map<Point, Region> tempPointRegionMap, List<Region> newRegions) {
        for (Region region : newRegions) {
            for (Berth berth : region.getBerths()) {
                Point topLeft = berth.pos;
                for (int dx = 0; dx < 4; dx++) {
                    for (int dy = 0; dy < 4; dy++) {
                        tempPointRegionMap.put(new Point(topLeft.x + dx, topLeft.y + dy), region);
                    }
                }
            }
        }
    }

    /**********************************************************************************
     * 单元测试打印接口，用于打印最后结果的正确性。
     * @function printRegionDetails: 打印region的大致信息，区域、点和坐标
     * @function printPointDetails: 打印每个区域具体有多少点
     * @function printPointToBerthPathsDetails: 打印所有点到泊位的路径
     * @function printBerthToPointPathsDetails: 打印所有泊位到点的路径
     * *********************************************************************************
     */
    public void printRegionDetails() {
        System.out.println("Total regions: " + regions.size());
        for (Region region : regions) {
            System.out.println("Region ID: " + region.getId());
            System.out.println("  Berths in region: " + region.getBerths().size());
            for (Berth berth : region.getBerths()) {
                System.out.println("    Berth at: " + berth.pos);
            }
            System.out.println("  Accessible points in region: " + region.getAccessiblePoints().size());
            System.out.println("  Assigned robots in region: " + region.getAssignedRobots().size());
        }
    }

    private int getShortestDistanceBetweenRegions(Region region1, Region region2) {
        int shortestDistance = unreachableFps;

        for (Berth berth1 : region1.berths) {
            for (Berth berth2 : region2.berths) {
                List<Point> path = berth1.mapPath.get(berth2.pos);
                if (path != null && path.size() < shortestDistance) {
                    shortestDistance = path.size();
                }
            }
        }

        return shortestDistance;
    }

    public void printNeighborRegionsDetails() {
        System.out.println("Total regions: " + regions.size());
        for (Region region : regions) {
            System.out.println("Region ID: " + region.id);
            System.out.println("  Neighbor regions and distances:");

            for (Region neighborRegion : region.neighborRegions) {
                int distance = getShortestDistanceBetweenRegions(region, neighborRegion);
                System.out.println("    Neighbor Region ID: " + neighborRegion.id + ", Distance: " + distance);
            }
        }
    }

    public void printPointDetails() {
        try (PrintWriter writer = new PrintWriter(new File("point.txt"))) {
            writer.println("Total regions: " + regions.size());
            for (Region region : regions) {
                writer.println("Region ID: " + region.getId());
                writer.println("  Berths in region: " + region.getBerths().size());
                for (Berth berth : region.getBerths()) {
                    writer.println("    Berth at: " + berth.pos);
                }
                writer.println("  Accessible points in region: " + region.getAccessiblePoints().size());
                if (!region.getAccessiblePoints().isEmpty()) {
                    writer.print("    Accessible Points: ");
                    for (Point point : region.getAccessiblePoints()) {
                        writer.print(point + " ");
                    }
                    writer.println();  // Move to the next line after printing all points of the region.
                }
                writer.println("  Assigned robots in region: " + region.getAssignedRobots().size());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void printZoneDetails() {
        System.out.println("Zone details:");
        for (Zone zone : zones) {
            System.out.println("Zone ID: " + zone.id);
            System.out.println("  Number of Regions: " + zone.regions.size());
            for (Region region : zone.regions) {
                System.out.println("    Region ID: " + region.getId());
                System.out.println("    Number of Points: " + region.getAccessiblePoints().size());
            }
        }
    }


    public void printBerthToPointPathsDetails() {
        String fileName = "berth2point.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write("Paths from Berths to Points:\n");
            for (Berth berth : berths) {
                Map<Point, List<Point>> paths = berth.mapPath;
                if (paths != null) {
                    for (Map.Entry<Point, List<Point>> entry : paths.entrySet()) {
                        Point point = entry.getKey();
                        List<Point> path = entry.getValue();
                        writer.write("Path from Berth " + berth.pos + " to " + point + ":\n");
                        for (Point pathPoint : path) {
                            writer.write(pathPoint + " ");
                        }
                        writer.newLine();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
