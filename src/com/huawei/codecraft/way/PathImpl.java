package com.huawei.codecraft.way;

import com.huawei.codecraft.util.Point;

import java.util.*;

import static com.huawei.codecraft.Const.unreachableFps;
import static com.huawei.codecraft.Util.printLog;
import static com.huawei.codecraft.way.Mapinfo.isValid;
import static com.huawei.codecraft.way.Mapinfo.map;

/**
 * ClassName: PathImpl
 * Package: com.huawei.codecraft.way
 * Description: 寻路的具体接口实现
 */
public class PathImpl implements Path{
    private static final int[][] directions = {{1, 0}, {-1, 0}, {0, -1}, {0, 1}};

    @Override
    public ArrayList<Point> getPath(Point p1, Point p2) {
//        long startTime = System.nanoTime();  // Start timing

        if (!isAccessible(p1.x, p1.y) || !isAccessible(p2.x, p2.y)) {
//            printLog("point is impossible");
            return null;
        }

        PriorityQueue<Pos> openSet = new PriorityQueue<>(Comparator.comparingInt(Pos::f));
        Map<Point, Pos> allNodes = new HashMap<>();

        Pos start = new Pos(p1, null, 0, estimateHeuristic(p1, p2));
        Pos end = new Pos(p2, null, 0, 0);

        openSet.add(start);
        allNodes.put(p1, start);

        while (!openSet.isEmpty()) {
            Pos current = openSet.poll();

            if (current.pos.equals(p2)) {
//                printLog("get path success");
//                long endTime = System.nanoTime();  // End timing
//                printLog("get path success, Time taken: " + (endTime - startTime) + " ns");
                return constructPath(current);
            }

            for (int[] direction : directions) {
                Point newPoint = new Point(current.pos.x + direction[0], current.pos.y + direction[1]);

                if (!isAccessible(newPoint.x, newPoint.y)) {
                    continue;
                }

                int newG = current.g + 1;  // 每一步代价为1
                if (!allNodes.containsKey(newPoint) || newG < allNodes.get(newPoint).g) {
                    int newH = estimateHeuristic(newPoint, p2);
                    Pos next = new Pos(newPoint, current, newG, newH);
                    openSet.add(next);
                    allNodes.put(newPoint, next);
                }
            }
        }

//        printLog("No way");
        return null;
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
    public ArrayList<Point> getToBerthPath(Point robotPos, Point BerthPoint) {
        Point target = offsetBerthPoint(robotPos, BerthPoint);
        return getPath(robotPos, target);
    }

    @Override
    public ArrayList<Point> getPathWithBarrier(Point p1, Point p2, HashSet<Point> barriers) {
        if (barriers.contains(p1)){
            barriers.remove(p1);
        }
        ArrayList<Point> blocks = new ArrayList<Point>(barriers);
        changeMapinfo(blocks);
        ArrayList<Point> path =  getPath(p1, p2);
        restoreMapinfo(blocks);
        return path;
    }

    @Override
    public ArrayList<Point> getHidePointPath(Point pos, ArrayList<Point> leftPath) {
        if (leftPath.size() < 2) {
            printLog("Error: leftPath does not contain enough points");
            return null;
        }
        ArrayList<Point> barriers = new ArrayList<Point>();
        barriers.add(leftPath.get(0));
        if (!pos.equals(leftPath.get(1))) {
            barriers.add(leftPath.get(1));
        }

        // 创建一个空的HashSet<Point>
        HashSet<Point> pointHashSet = new HashSet<>(barriers);

        // 在地图上临时标记对方机器人下一帧的位置为障碍物
        changeMapinfo(barriers);
        ArrayList<Point> path = null;
        // 遍历可能的避让方向
        for (int[] direction : directions) {
            int newX = pos.x + direction[0];
            int newY = pos.y + direction[1];
            Point newTarget = new Point(newX, newY);

            // 检查新目标点是否可达且不是leftPath的一部分
            if (isAccessible(newX, newY) && !pointHashSet.contains(newTarget)) {
                path = getPath(pos, newTarget);  // 尝试找到一条避让路径
                // 如果找到有效路径且路径终点不在leftPath中，则返回该路径
                if (path != null && !path.isEmpty()) {
                    break;
                } else {
                    path = null;  // 没有找到有效路径，继续尝试
                }
            }
        }

        // 恢复地图信息
        restoreMapinfo(barriers);

        if (path == null) {
            printLog("No valid hide point found");
        }
        return path;
    }

    // 修改地图信息以添加障碍物
    private void changeMapinfo(ArrayList<Point> barriers) {
        for (Point barrier : barriers) {
            if ( map[barrier.x][barrier.y] == -2) {
                printLog("leftpath error,obstacle");
            }
//            printLog("change pos" + barrier + " obstacle");
            map[barrier.x][barrier.y] = -2;  // 标记障碍物
        }
    }

    // 恢复地图信息
    private void restoreMapinfo(ArrayList<Point> barriers) {
        for (Point barrier : barriers) {
            map[barrier.x][barrier.y] = 0;  // 恢复为陆地
//            printLog("restore pos" + barrier + " land");
        }
    }

    private int estimateHeuristic(Point p1, Point p2) {
        // Example using Manhattan distance
        return Math.abs(p1.x - p2.x) + Math.abs(p1.y - p2.y);
    }

    private static Point offsetBerthPoint(Point robotPos, Point BerthPoint) {
        // 假设最近泊位点为离机器人最近的泊位点
        Point target = new Point();

        if (robotPos.x > BerthPoint.x + 3) {
            target.x = BerthPoint.x + 3;
        } else {
            target.x = Math.max(robotPos.x, BerthPoint.x);
        }
        if (robotPos.y > BerthPoint.y + 3) {
            target.y = BerthPoint.y + 3;
        }
        else {
            target.y = Math.max(robotPos.y, BerthPoint.y);
        }

        return  target;
    }

    private static boolean isAccessible(int x, int y) {
        return isValid(x, y) && map[x][y] == 0;
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


