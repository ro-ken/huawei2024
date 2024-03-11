package com.huawei.codecraft.way;

import com.huawei.codecraft.core.Berth;
import com.huawei.codecraft.util.Point;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static com.huawei.codecraft.Const.*;
import static com.huawei.codecraft.way.Mapinfo.isValid;
import static com.huawei.codecraft.way.Mapinfo.map;

/**
 * ClassName: RegionManager
 * Package: com.huawei.codecraft.way
 * Description: 管理region，向上提供接口
 */
public class RegionManager {
    private final List<Region> regions;
    private final Map<Point, Region> pointRegionMap;
    private final Map<Point, Map<Berth, List<Point>>> globalPointToPathMap;
    private final Map<Berth, Map<Point, List<Point>>> globalBerthToPointPaths;
    private final Map<Point, Map<Berth, List<Point>>> globalPointToBerthPaths;
    private final Path pathFinder;  // Path 接口的引用

    public RegionManager(Path pathFinder) {
        this.regions = new ArrayList<>();
        this.pointRegionMap = new HashMap<>();
        this.pathFinder = pathFinder;  // 通过构造器注入 Path 实现
        this.globalPointToPathMap = new HashMap<>();
        this.globalBerthToPointPaths = new HashMap<>();
        this.globalPointToBerthPaths = new HashMap<>();
//        createRegions();
//        getFullPath();
//        splitRegions();
    }

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
     * 创建初始的连通区域，根据地图的联通性划分
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

    public Region getRegionByPoint(Point point) {
        return pointRegionMap.get(point);
    }

    // 预先获取全局地图上所有的路径，点到泊位和泊位到点
    public void getFullPath() {
        // 为每个泊位计算到所有其他点的路径
        for (Berth berth : berths) {
            Map<Point, List<Point>> berthPaths = new HashMap<>();
            bfsFromBerthForFullPaths(berth, berthPaths);
            globalBerthToPointPaths.put(berth, berthPaths);

            // 根据计算出的泊位到点的路径构建点到泊位的路径
            for (Map.Entry<Point, List<Point>> entry : berthPaths.entrySet()) {
                Point point = entry.getKey();
                List<Point> path = entry.getValue();
                List<Point> reversedPath = new ArrayList<>(path);
                Collections.reverse(reversedPath); // 反转路径

                globalPointToBerthPaths.computeIfAbsent(point, k -> new HashMap<>());
                globalPointToBerthPaths.get(point).put(berth, reversedPath);
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

    // 使用并查集进行聚类
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

    // 进一步划分区域
    public void splitRegions() {
        List<Region> newRegions = new ArrayList<>();
        for (Region largeRegion : regions) {
            // 获取泊位对应的所有点
            Set<Point> points = largeRegion.getBerths().stream().map(berth -> berth.pos).collect(Collectors.toSet());

            // 初始化并查集
            UnionFind unionFind = new UnionFind(points);

            // 计算泊位间的距离并确定阈值
            int threshold = calculateThreshold(largeRegion);
//             System.out.println("****threshold: " + threshold);
            // 基于阈值合并泊位到新区域
            mergeBerths2NewRegions(largeRegion, threshold, unionFind, newRegions);

            // 将大区域内剩余的点分配到最近的新区域
            allocateRemainingPoints(largeRegion, newRegions);
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
        int thresholdIndex = (int) (distances.size() * 0.1);  // 调整百分比以得到所需阈值大小
        return distances.get(thresholdIndex);
    }


    // 将根据阈值划分的泊位进行聚合，定义新的region
    private void mergeBerths2NewRegions(Region largeRegion, int threshold, UnionFind unionFind, List<Region> newRegions) {
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
            }
            Region region = rootToRegion.get(root);
            Berth berth = pointToBerth.get(point);
            if (berth != null && !region.getBerths().contains(berth)) {
                region.addBerth(berth);
            }
        }
    }

    private void allocateRemainingPoints(Region largeRegion, List<Region> newRegions) {
//        Map<Point, Region> tempPointRegionMap = new HashMap<>();
//        addBerthArea(tempPointRegionMap, newRegions);
        for (Berth berth : largeRegion.getBerths()) {
            bfsFromBerth(berth);
        }
        // 根据bfs得到点到泊位的最短路进行划分点的所属区域
        allocatepoint2region(newRegions);
    }

    private void bfsFromBerth(Berth berth) {
        Queue<Point> queue = new LinkedList<>();
        Point start = berth.pos;
        queue.add(start);

        Map<Point, List<Point>> visitedPaths = new HashMap<>();
        visitedPaths.put(start, new ArrayList<>());
        visitedPaths.get(start).add(start);

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

                    globalPointToPathMap.compute(nextPoint, (k, v) -> {
                        // 如果还没有存储任何到达nextPoint的路径，或者找到了一条更短的路径
                        if (v == null) {
                            v = new HashMap<>();
                            v.put(berth, newPath);
                        } else {
                            List<Point> currentShortestPath = v.get(berth);
                            // 如果对应berth的当前最短路径不存在或者新路径更短
                            if (currentShortestPath == null || newPath.size() < currentShortestPath.size()) {
                                v.put(berth, newPath);
                            }
                        }
                        return v;
                    });

                    visitedPaths.put(nextPoint, newPath);
                    queue.add(nextPoint);
                }
            }
        }
    }

    private void allocatepoint2region(List<Region> newRegions) {
        for (Map.Entry<Point, Map<Berth, List<Point>>> entry : globalPointToPathMap.entrySet()) {
            Point point = entry.getKey();
            Map<Berth, List<Point>> berthPathMap = entry.getValue();
            Berth closestBerth = Collections.min(berthPathMap.entrySet(), Comparator.comparingInt(e -> e.getValue().size())).getKey();
            Region region = findRegionByBerth(closestBerth, newRegions);
            if (region != null) {
                region.addAccessiblePoint(point);
            }
        }
    }

    private Region findRegionByBerth(Berth berth, List<Region> newRegions) {
        for (Region region : newRegions) {
            if (region.getBerths().contains(berth)) {
                return region;
            }
        }
        return null;
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

    public void printRegionZeroPathsDetails() {
        // 假定0号区域存在
        Region regionZero = regions.get(0);
        String fileName = "path.txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            if (regionZero == null) {
                writer.write("Region 0 does not exist.");
                return;
            }

            writer.write("Paths in Region 0:\n");
            for (Point point : regionZero.getAccessiblePoints()) {
                // 获取该点到不同泊位的路径
                Map<Berth, List<Point>> paths = globalPointToPathMap.get(point);
                if (paths != null) {
                    for (Map.Entry<Berth, List<Point>> entry : paths.entrySet()) {
                        Berth berth = entry.getKey();
                        List<Point> path = entry.getValue();
                        writer.write("Path from " + point + " to Berth " + berth.pos + ":\n");
                        for (Point pathPoint : path) {
                            writer.write(pathPoint + " ");
                        }
                        writer.write("\n");  // 换行，分隔不同路径
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void printPointToBerthPathsDetails() {
        String fileName = "point2berth.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write("Paths from Points to Berths:\n");
            for (Map.Entry<Point, Map<Berth, List<Point>>> entry : globalPointToBerthPaths.entrySet()) {
                Point point = entry.getKey();
                Map<Berth, List<Point>> paths = entry.getValue();
                for (Map.Entry<Berth, List<Point>> pathEntry : paths.entrySet()) {
                    Berth berth = pathEntry.getKey();
                    List<Point> path = pathEntry.getValue();
                    writer.write("Path from " + point + " to Berth " + berth.pos + ":\n");
                    for (Point pathPoint : path) {
                        writer.write(pathPoint + " ");
                    }
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void printBerthToPointPathsDetails() {
        String fileName = "berth2point.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write("Paths from Berths to Points:\n");
            for (Berth berth : berths) {
                Map<Point, List<Point>> paths = globalBerthToPointPaths.get(berth);
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
