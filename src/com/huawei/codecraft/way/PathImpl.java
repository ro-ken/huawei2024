package com.huawei.codecraft.way;

import com.huawei.codecraft.util.Point;

import java.util.*;

import static com.huawei.codecraft.Const.*;
import static com.huawei.codecraft.Util.*;
import static com.huawei.codecraft.way.Mapinfo.isValid;
import static com.huawei.codecraft.way.Mapinfo.map;
import static com.huawei.codecraft.way.Mapinfo.seaMap;

/**
 * ClassName: PathImpl
 * Package: com.huawei.codec raft.way
 * Description: 寻路的具体接口实现
 */
public class PathImpl implements Path {
    private static final int[][] directions = {{1, 0}, {-1, 0}, {0, -1}, {0, 1}};
    private static final int clockwise = 0; // 顺时针
    private static final int counterClockwise = 1;  // 逆时针
    public  static final int shipLen = 3;
    public static Point[] ship = new Point[shipLen];
    private static final int[][][] turnTimes = {
            {{4, 2 , 3, 1},{4, 2, 1, 3}}, // turnTimes[][0] 代表顺时针转换次数，turnTimes[][1]代表逆时针转换次数
            {{2, 4 , 1, 3},{2, 4, 3, 1}},
            {{1, 3 , 4, 2},{3, 1, 4, 2}},
            {{3, 1 , 2, 4},{1, 3, 2, 4}}
    };

    private static final Map<Integer, Integer> clockwiseRotation = new HashMap<>();
    private static final Map<Integer, Integer> counterClockwiseRotation = new HashMap<>();

    static {
        clockwiseRotation.put(0, 3);
        clockwiseRotation.put(1, 2);
        clockwiseRotation.put(2, 0);
        clockwiseRotation.put(3, 1);

        counterClockwiseRotation.put(0, 2);
        counterClockwiseRotation.put(1, 3);
        counterClockwiseRotation.put(2, 1);
        counterClockwiseRotation.put(3, 0);
    }

    private static final int[][][] clockwiseCoordinate = {  // 顺时针转时坐标变化
            {{0, 0},{2, 2}, {2, 0}, {0, 2}}, // clockwiseCoordinate[][0] 代表x，clockwiseCoordinate[][1]代表y
            {{-2, -2},{0, 0}, {0, -2}, {-2, 0}},
            {{-2, 0},{0, 2}, {0, 0}, {-2, 2}},
            {{0, -2},{2, 0}, {2, -2}, {0, 0}}
    };

    private static final int[][][] counterClockwiseCoordinate = {  // 顺时针转时坐标变化
            {{0, 0},{0, 2}, {1, 1}, {-1, 1}}, // counterClockwiseCoordinate[][0] 代表x，counterClockwiseCoordinate[][1]代表y
            {{2, 0},{0, 0}, {-1, -1}, {1, -1}},
            {{-1, -1},{-1, 1}, {0, 0}, {-2, 0}},
            {{1, -1},{1, 1}, {2, 0}, {0, 0}}
    };

    public PathImpl() {

    }

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
    // TODO：暂时不考虑主干路减速的存在，之后寻路时再优化
    public ArrayList<Point> getBoatPath(Point core, int direction, Point dest) {
        ArrayList<Point> path = new ArrayList<>();
       // 陆地寻路，直接按照 manhadun 距离进行搜索即可
        // 优先走水平，再走垂直，这样来回路径默认错开
        int horizon = judgeHorizon(core, dest);         // 判断水平方向，开始确定唯一
        int vertical = judgeVertical(core, dest);       // 判断垂直方向, 开始确定唯一
        int times = 0;
        initShip(core);
        refreshShip(core, direction);
        path.add(new Point(core));
        while (!ship[2].equals(dest) && times < 2000) {
            times += 1;
            if (canMoveHorizon(horizon, direction, dest)) { // 走水平
                if (direction != horizon) {   // 方向改为水平
                    turnDirection(direction, horizon, dest, path);
                    direction = horizon;
                }
                pushForward(horizon, path);
            }
            else { // 走垂直
                if (direction != vertical) { // 方向改为垂直
                    turnDirection(direction, vertical, dest, path);
                    direction = vertical;
                }
                pushForward(vertical, path);
            }
        }
        if (!ship[2].equals(dest)) {
            System.out.println("get path error");
            return null;
        }
        // 因为是前置节点到达了终点，所以需要加上去
        path.add(new Point(ship[1]));
        path.add(new Point(ship[2]));
        return path;
    }

    private void initShip(Point core) {
        for (int i = 0; i < shipLen; i++) {
            ship[i] = new Point(core);
        }
    }

    // 根据当前方向获取船只核心点方向上得节点，ship[0] 对应核心点 core
    private void refreshShip(Point core, int direction) {
        for (int i = 0; i < shipLen; i++) {
            if (direction == LEFT) {
                ship[i].y = core.y - i;
                ship[i].x = core.x;
            }
            else if (direction == RIGHT) {
                ship[i].y = core.y + i;
                ship[i].x = core.x;
            }
            else if (direction == UP) {
                ship[i].x = core.x - i;
                ship[i].y = core.y;
            }
            else {
                ship[i].x = core.x + i;
                ship[i].y = core.y;
            }
        }
    }

    private  boolean canMoveHorizon(int destDirection, int direction, Point dest) {
        if (direction == destDirection) {
            if (destDirection == LEFT) { //目标方向向左，当前方向 x不可以小于 dest.x
                return isValid(ship[2].x, ship[2].y - 1) &&  seaMap[ship[2].x][ship[2].y - 1] != ROAD &&
                        isValid(ship[2].x - 1, ship[2].y - 1) && (seaMap[ship[2].x - 1][ship[2].y - 1] != ROAD)
                        && ship[2].y > dest.y ;
            }
            else {
                return isValid(ship[2].x, ship[2].y + 1) && seaMap[ship[2].x][ship[2].y + 1] != ROAD &&
                        isValid(ship[2].x + 1, ship[2].y + 1) && seaMap[ship[2].x + 1][ship[2].y + 1] != ROAD &&
                        ship[2].y < dest.y;
            }
        }
        // 不同向需要旋转才可以
        if (direction == UP) {
            if (destDirection == LEFT) {
                return canCounterClockwiseTurn(direction) && ship[2].y > dest.y;
            }
            else {
                return canClockwiseTurn(direction) && ship[2].y < dest.y;
            }
        }
        else {
            if (destDirection == LEFT) {
                return canClockwiseTurn(direction) && ship[2].y > dest.y;
            }
            else {
                return canCounterClockwiseTurn(direction)  && ship[2].y < dest.y;
            }
        }
    }

    private void turnDirection(int direction, int newDirection, Point dest,ArrayList<Point> path) {
        // 判断逆时针和顺时针的次数，哪个少转哪个，如果都一样就看哪个能转
        if (turnTimes[direction][clockwise][newDirection] <= turnTimes[direction][counterClockwise][newDirection]) {
            if (canClockwiseTurn(direction)) {
                turnClockwise(direction, newDirection, path);
            }
            else {
                turnCounterClockwise(direction, newDirection, dest, path);
            }
        }
        else {
            if (canCounterClockwiseTurn(direction)) {
                turnCounterClockwise(direction, newDirection, dest, path);
            }
            else {
                turnClockwise(direction, newDirection, path);
            }
        }
    }

    private boolean canClockwiseTurn(int direction) {
        // 顺时针，坐标会沿着角落进行轮转，判断角坐标是否存在不可达点
        for (int i = 0; i <= 3; i++) {
            int x = ship[0].x + clockwiseCoordinate[direction][i][0];
            int y = ship[0].y + clockwiseCoordinate[direction][i][1];
            if (isValid(x, y) && seaMap[x][y] == ROAD) {
                return false;
            }
        }
        return true;
    }

    private boolean canCounterClockwiseTurn(int direction) {
        // 逆时针，判断核心点上面
        // 顺时针，坐标会沿着角落进行轮转，判断角坐标是否存在不可达点
        for (int i = 0; i <= 3; i++) {
            int x = ship[0].x + counterClockwiseCoordinate[direction][i][0];
            int y = ship[0].y + counterClockwiseCoordinate[direction][i][1];
            if (isValid(x, y) && seaMap[x][y] == ROAD) {
                return false;
            }
        }
        return true;
    }

    private void  turnClockwise(int direction, int newDirection, ArrayList<Point> path) {
        // 顺时针旋转，核心点转之后刚好能够转到前置Point得位置
        while (direction != newDirection) {
            direction = clockwiseRotation.get(direction);
            ship[0].x = ship[2].x;
            ship[0].y = ship[2].y;

            // 更新ship得前驱节点
            refreshShip(ship[0], direction);
            path.add(new Point(ship[0]));
        }
    }

    private void  turnCounterClockwise(int direction, int newDirection, Point dest, ArrayList<Point> path) {
        while (direction != newDirection) {
            int tempDirection = counterClockwiseRotation.get(direction);    // 获取下一个方向
            // 逆时针旋转，则需要特殊得处理
            if (ship[2].x == dest.x || ship[2].y == dest.y) {
                path.add(new Point(ship[1]));

                // 以 ship 1 位置进行逆时针旋转刚好使得 x y 对齐
                ship[0].x = ship[1].x + counterClockwiseCoordinate[direction][tempDirection][0];
                ship[0].y = ship[1].y + counterClockwiseCoordinate[direction][tempDirection][1];
            }
            else {
                ship[0].x = ship[0].x + counterClockwiseCoordinate[direction][tempDirection][0];
                ship[0].y = ship[0].y + counterClockwiseCoordinate[direction][tempDirection][1];
            }

            // 更新ship得前驱节点
            refreshShip(ship[0], tempDirection);

            path.add(new Point(ship[0]));
            direction = tempDirection;
        }
    }

    private void pushForward(int direction,  ArrayList<Point> path) {
        if (direction == LEFT) {
            ship[0].y -= 1;
        }
        else if (direction == RIGHT) {
            ship[0].y += 1;
        }
        else if (direction == UP) {
            ship[0].x -= 1;
        }
        else {
            ship[0].x += 1;
        }
        refreshShip(ship[0], direction);
        path.add(new Point(ship[0]));
   }

    private  int judgeVertical(Point core, Point dest) {
        if (core.x < dest.x) {
            return DOWN;
        }
        else {
            return UP;
        }
    }

    private  int judgeHorizon(Point core, Point dest) {
        if (core.y < dest.y) {
            return RIGHT;
        }
        else {
            return LEFT;
        }
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


