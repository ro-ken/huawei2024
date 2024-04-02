package com.huawei.codecraft.way;

import com.huawei.codecraft.util.Point;

import java.util.*;

import static com.huawei.codecraft.Const.*;
import static com.huawei.codecraft.Util.*;
import static com.huawei.codecraft.way.Mapinfo.isValid;
import static com.huawei.codecraft.way.Mapinfo.map;

/**
 * ClassName: PathImpl
 * Package: com.huawei.codec raft.way
 * Description: 寻路的具体接口实现
 */
public class PathImpl implements Path {
    private static final int[][] directions = {{1, 0}, {-1, 0}, {0, -1}, {0, 1}};

    private  ArrayList<Point> getPathWithLimit(Point p1, Point p2, int limitLen) {
//        long startTime = System.nanoTime();  // Start timing
        if (!isAccessible(p1.x, p1.y) || !isAccessible(p2.x, p2.y)) {
            printLog("point is impossible");
            return null;
        }
        PriorityQueue<Pos> openSet = new PriorityQueue<>(Comparator.comparingInt(Pos::f));
        Map<Point, Pos> visitedNodes = new HashMap<>();

        Pos start = new Pos(p1, null, 0, estimateHeuristic(p1, p2));

        openSet.add(start);
        visitedNodes.put(p1, start);

        while (!openSet.isEmpty()) {
            Pos current = openSet.poll();
            if (current.pos.equals(p2)) {
//                long endTime = System.nanoTime();  // End timing
//                printLog("get path success, Time taken: " + (endTime - startTime) + " ns");
                return constructPath(current);
            }
            if (limitLen != NoLimit && current.g > limitLen) {
                printDebug("road too long, stop find");
                return null;
            }
            for (int[] direction : directions) {
                Point newPoint = new Point(current.pos.x + direction[0], current.pos.y + direction[1]);

                if (!isAccessible(newPoint.x, newPoint.y)) {
                    continue;
                }

                int newG = current.g + 1;  // 每一步代价为1
                if (!visitedNodes.containsKey(newPoint) || newG < visitedNodes.get(newPoint).g) {
                    int newH = estimateHeuristic(newPoint, p2);
                    Pos next = new Pos(newPoint, current, newG, newH);
                    openSet.add(next);
                    visitedNodes.put(newPoint, next);
                }
            }
        }

        printLog("No way");
        return null;
    }
    @Override
    public ArrayList<Point> getPath(Point p1, Point p2) {
          return getPathWithLimit(p1, p2, NoLimit);
    }

    @Override
    public int getPathFps(Point p1, Point p2) {
        ArrayList<Point> path = getPath(p1, p2);
        if (path == null) {
            return unreachableFps;
        }
        return path.size();
    }

    @Override
    public ArrayList<Point> getPathWithBarrier(Point p1, Point p2, HashSet<Point> barriers) {
        barriers.remove(p1);
        ArrayList<Point> blocks = new ArrayList<>(barriers);
        changeMapinfo(blocks);
        ArrayList<Point> path =  getPath(p1, p2);
        restoreMapinfo(blocks);
        return path;
    }

    @Override
    public ArrayList<Point> getPathWithBarrierWithLimit(Point pos, Point target, HashSet<Point> barriers, int maxLen) {
        barriers.remove(pos);
        ArrayList<Point> blocks = new ArrayList<>(barriers);
        changeMapinfo(blocks);
        if (maxLen < 2) {
            printErr("too short length");
        }
        ArrayList<Point> path =  getPathWithLimit(pos, target, maxLen - 2);
        restoreMapinfo(blocks);
        return path;
    }

    @Override
    public ArrayList<Point> getHidePointPath(Point startPoint, List<Point> leftPath) {
        if (leftPath.size() < 2) {
            printErr("leftPath does not contain enough points");
            return null;
        }
        ArrayList<Point> path = null;
        ArrayList<Point> barriers = new ArrayList<>();
        // 创建一个空的HashSet<Point>
        HashSet<Point> visited = new HashSet<>();
        visited.add(leftPath.get(0));

        barriers.add(leftPath.get(0));
        if (!startPoint.equals(leftPath.get(1))) {
            barriers.add(leftPath.get(1));
            visited.add(leftPath.get(1));
        }

        // 创建 leftPath 的hashSet
        HashSet<Point> pointHashSet = new HashSet<>(leftPath);
        Pos startPos = new Pos(startPoint, null, 0, 0);

        // bfs 队列
        Queue<Pos> queue = new LinkedList<>();
        queue.add(startPos);

        // 遍历之前将地图临时修改为障碍物，防止对位遍历
        changeMapinfo(barriers);

        // 遍历可能的避让方向
        while (!queue.isEmpty()) {
            Pos current = queue.poll();
            // 点是可达点且未被访问过
            if (isAccessible(current.pos.x, current.pos.y) && !visited.contains(current.pos)) {
                if (!pointHashSet.contains(current.pos) || isHidePoint(current.pos)) {
                    path = constructPath(current);  // 尝试找到一条避让路径
                    // 如果找到有效路径且路径终点不在leftPath中，则返回该路径
                    break;
                }
                // 添加点为已探索
                visited.add(current.pos);
                for (int[] direction : directions) {
                    Point newPoint = new Point(current.pos.x + direction[0], current.pos.y + direction[1]);
                    Pos next = new Pos(newPoint, current, 0, 0);
                    queue.add(next);
                }
            }
        }

        // 恢复地图信息
        restoreMapinfo(barriers);

        if (path == null) {
            printLog("No valid hide point found");
//            System.out.println("No valid hide point found");
        }
        return path;
    }

    @Override
    public ArrayList<Point> getBoatPath(Point core, int direction, Point dest) {
        return null;
    }

    private boolean isHidePoint(Point point) {
        return map[point.x][point.y] == MAINROAD || map[point.x][point.y] == MAINBOTH;
    }

    // 修改地图信息以添加障碍物
    private void changeMapinfo(ArrayList<Point> barriers) {
        for (Point barrier : barriers) {
            if (map[barrier.x][barrier.y] == ROAD) { // 将陆地暂时变为障碍物
                map[barrier.x][barrier.y] = OBSTACLE;  // 标记障碍物
            }
        }
    }

    // 恢复地图信息
    private void restoreMapinfo(ArrayList<Point> barriers) {
        for (Point barrier : barriers) {
           if (map[barrier.x][barrier.y] == OBSTACLE) {
               map[barrier.x][barrier.y] = ROAD;  // 恢复为陆地
           }
//            printLog("restore pos" + barrier + " land");
        }
    }

    private int estimateHeuristic(Point p1, Point p2) {
        // Example using Manhattan distance
        return Math.abs(p1.x - p2.x) + Math.abs(p1.y - p2.y);
    }

    private static boolean isAccessible(int x, int y) {
        return isValid(x, y) && map[x][y] >= MAINBOTH;
    }

    private static ArrayList<Point> constructPath(Pos end) {
        ArrayList<Point> path = new ArrayList<>();
        for (Pos p = end; p != null; p = p.father) {
            path.add(p.pos);
        }
        Collections.reverse(path);
        return path;
    }
}


