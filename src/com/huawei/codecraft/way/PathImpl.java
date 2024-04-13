package com.huawei.codecraft.way;

import com.huawei.codecraft.util.Point;
import com.huawei.codecraft.util.Twins;

import java.util.*;

import static com.huawei.codecraft.Const.*;
import static com.huawei.codecraft.Util.*;
import static com.huawei.codecraft.Util.printLog;
import static com.huawei.codecraft.way.Mapinfo.*;

/**
 * ClassName: PathImpl
 * Package: com.huawei.codec raft.way
 * Description: 寻路的具体接口实现
 */
public class PathImpl implements Path {
    private static final int[][] directions = {{0, 1}, {0, -1}, {-1, 0}, {1, 0}}; // 右、左、上、下
    private static final int clockwise = 0; // 顺时针
    private static final int counterClockwise = 1;  // 逆时针
    private static final int robot = 0;  // 机器人
    private static final int boat = 1;  // 船
    private static final int special = 2;  // 特殊点特殊处理
    public  static final int shipLen = 3;
    public static Point[] ship = new Point[shipLen];
    public static HashMap<Point, Integer> specialPoint = new HashMap<>();
    public static ArrayList<Point> blockPoints = new ArrayList<>();
    public static HashMap<Point, Integer> blockPointsMap = new HashMap<>();
    private static final int[][][] turnTimes = {
            {{4, 2 , 3, 1},{4, 2, 1, 3}}, // turnTimes[][0] 代表顺时针转换次数，turnTimes[][1]代表逆时针转换次数
            {{2, 4 , 1, 3},{2, 4, 3, 1}},
            {{1, 3 , 4, 2},{3, 1, 4, 2}},
            {{3, 1, 2, 4},{1, 3, 2, 4}}
    };

    public static final Map<Integer, Integer> clockwiseRotation = new HashMap<>(); // 轮转方向顺时针映射
    public static final Map<Integer, Integer> counterClockwiseRotation = new HashMap<>(); // 轮转方向逆时针映射

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
            {{0, -2},{0, 0}, {1, -1}, {-1, -1}},
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

        Pos start = new Pos(p1, null, 0, calculateDistance(p1, p2));

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
                    int newH = calculateDistance(newPoint, p2);
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
        changeMapinfo(blocks, robot);
        ArrayList<Point> path =  getPath(p1, p2);
        restoreMapinfo(blocks, robot);
        return path;
    }

    @Override
    public ArrayList<Point> getPathWithBarrierWithLimit(Point pos, Point target, HashSet<Point> barriers, int maxLen) {
        barriers.remove(pos);
        ArrayList<Point> blocks = new ArrayList<>(barriers);
        changeMapinfo(blocks, robot);
        if (maxLen < 2) {
            printErr("too short length");
        }
        ArrayList<Point> path =  getPathWithLimit(pos, target, maxLen - 2);
        restoreMapinfo(blocks, robot);
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
        changeMapinfo(barriers, robot);

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
        restoreMapinfo(barriers, robot);

        if (path == null) {
            printLog("No valid hide point found");
//            System.out.println("No valid hide point found");
        }
        return path;
    }

    // TODO：暂时不考虑主干路减速的存在，之后寻路时再优化
    // 陆地寻路，直接按照曼哈顿距离进行搜索即可
    // 优先走水平，再走垂直，这样来回路径默认错开
    @Override
    public ArrayList<Point> getBoatPath(Point core, int direction, Point dest) {
        if (core.equals(dest)) {
            return new ArrayList<>();
        }
        // 小于6的距离认为没有障碍，直接寻路
        if (calculateDistance(core, dest) <= 3) {
            return getBoatPathSimply(core, direction, dest);
        }
        Point newDest;
        // 泊位终点尽可能不靠墙
        newDest = offsetDestination(dest);
        int vertical = judgeVertical(core, dest);
        int horizon = judgeHorizon(core, dest);
        int dir;
        // 朝向就是移动方向，一定朝外
        if (direction == vertical || direction == horizon) {
            dir = direction;
        }
        else {
            dir = getBestDir(core, vertical, horizon);
        }

        // 水平优先
        return getFinalPath(core, direction, newDest, dir);
    }

    private int getBestDir(Point core, int vertical, int horizon) {
        // 朝向vertical方向上的空地
        int verCnt = 0, horCnt = 0;
        int x = core.x, y = core.y;
        if (horizon == LEFT) {
            while (isValid(x, y) && horCnt < 6 && seaMap[x][y] != ROAD) {
                horCnt++;
                y -= 1;
            }
        }
        else {
            while (isValid(x, y) && horCnt < 6 && seaMap[x][y] != ROAD) {
                horCnt++;
                y += 1;
            }
        }
        x = core.x;
        y = core.y;
        if (vertical == UP) {
            while (isValid(x, y) && verCnt < 6 && seaMap[x][y] != ROAD) {
                verCnt++;
                x -= 1;
            }
        }
        else {
            while (isValid(x, y) && verCnt < 6 && seaMap[x][y] != ROAD) {
                verCnt++;
                x += 1;
            }
        }
        return verCnt > horCnt ? vertical : horizon;
    }

    public static int getPathLen(ArrayList<Point> finalPath) {
        int len = 0;
        int size = finalPath.size();
        if (size >= 2 && finalPath.get(size - 1).equals(finalPath.get(size - 2))) {
            size--;
        }
        for (int i = 0; i < size; i++) {
            Point p = finalPath.get(i);
            if (isValid(p.x, p.y) && (Mapinfo.map[p.x][p.y] == MAINBOTH || Mapinfo.map[p.x][p.y] == MAINSEA)) {
                len += 2;
            }
            else  if (isValid(p.x, p.y)){
                len += 1;
            }
        }
        return len;
    }

    @Override
    public Twins<ArrayList<Point>, Integer> getBoatPathAndFps(Point core, int direction, Point dest) {
        ArrayList<Point> finalPath = getBoatPath(core, direction, dest);
        int pathLen = getPathLen(finalPath);
        return new Twins<>(finalPath, pathLen);
    }

    private int getPassablePoint(Point start, int l, int r) {
        int cnt = 0; // 空格数
        int x = start.x,y = start.y;
        int xFlag = l /  Math.abs(l);  //计算是正还是负的标记
        int yFlag = r / Math.abs(r);
        for (int i = 0; i < Math.abs(l); i++) {
            for (int j = 0; j < Math.abs(r); j++) {
                if (isValid(x + i * xFlag, y + j * yFlag) && seaMap[x + i * xFlag][y + j * yFlag] != ROAD) {
                    cnt++;
                }
            }
        }
        return cnt;
    }

    private boolean noObstacle(Point start, int direction) {
        int cnt = 0;
        while (cnt < 4 && isValid(start.x, start.y) && seaMap[start.x][start.y] != ROAD) {
            getNextPoint(direction, start);
            cnt++;
        }
        return cnt == 5;
    }

    @Override
    public ArrayList<Point> getBoatPathWithBarrier(Point core, int direction, Point dest, HashSet<Point> points) {
        ArrayList<Point> barriers = new ArrayList<>(points);
        ArrayList<Point> boatBoatPath = new ArrayList<>();
        printLog("block Points:" + points);
        initShip(core);
        refreshShip(core, direction);
        // 先改地图信息
        changeMapinfo(barriers, boat);
        // 使用ship[1]，而不是用ship[0]
        int cnt1 = 0, cnt2 = 0;
        int nextDir;
        // 判断当前方向往前是不是障碍物
        if (noObstacle(new Point(ship[0]), direction)) {
            nextDir = direction;
        }
        else {
            if (direction == LEFT) {
                cnt1 = getPassablePoint(new Point(ship[1]), -6, -6);
                cnt2 = getPassablePoint(new Point(ship[1]), 6, -6);
                nextDir = cnt1 > cnt2 ? UP : DOWN;
            }
            else if (direction == RIGHT) {
                cnt1 = getPassablePoint(new Point(ship[1]), -6, 6);
                cnt2 = getPassablePoint(new Point(ship[1]), 6, 6);
                nextDir = cnt1 > cnt2 ? UP : DOWN;
            }
            else if (direction == UP) {
                cnt1 = getPassablePoint(new Point(ship[1]), -6, -6);
                cnt2 = getPassablePoint(new Point(ship[1]), -6, 6);
                nextDir = cnt1 > cnt2 ? LEFT : RIGHT;
            }
            else {
                cnt1 = getPassablePoint(new Point(ship[1]), 6, -6);
                cnt2 = getPassablePoint(new Point(ship[1]), 6, 6);
                nextDir = cnt1 > cnt2 ? LEFT : RIGHT;
            }
        }
        printLog("cnt1: " + cnt1 + "  cnt2: " + cnt2 + "  nextDir: " + nextDir + " direction: " + direction);
        // 哪边更宽阔，往那边转
        if (direction != nextDir) {
            turnDirection(direction, nextDir, null, boatBoatPath);
            direction = nextDir;
        }
        // 恢复地图
        restoreMapinfo(barriers, boat);
        // 获取剩余路径
        ArrayList<Point> leftPath = getFinalPath(ship[0], direction, dest, direction);
        if (leftPath == null) {
            return boatBoatPath;
        }
        if (boatBoatPath.size() != 0 && !boatBoatPath.get(boatBoatPath.size() - 1).equals(leftPath.get(0))) {
            boatBoatPath.add(leftPath.get(0));
        }
        for (int i = 1; i < leftPath.size(); i++) {
            boatBoatPath.add(leftPath.get(i));
        }
        return boatBoatPath;
    }

    private ArrayList<Point> getRoundPoints(int direction) {
        if (direction == LEFT || direction == RIGHT) {
            blockPoints.add(new Point(ship[0].x - 1, ship[0].y));
            blockPoints.add(new Point(ship[0].x + 1, ship[0].y));
            blockPoints.add(new Point(ship[1].x - 1, ship[1].y));
            blockPoints.add(new Point(ship[1].x + 1, ship[1].y));
            if (direction == LEFT) {
                blockPoints.add(new Point(ship[0].x, ship[0].y + 1));
            }
            else {
                blockPoints.add(new Point(ship[0].x, ship[0].y - 1));
            }
        }
        else  {
            blockPoints.add(new Point(ship[0].x, ship[0].y - 1));
            blockPoints.add(new Point(ship[0].x, ship[0].y + 1));
            blockPoints.add(new Point(ship[1].x, ship[1].y - 1));
            blockPoints.add(new Point(ship[1].x, ship[1].y + 1));
            if (direction == UP) {
                blockPoints.add(new Point(ship[0].x + 1, ship[0].y));
            }
            else {
                blockPoints.add(new Point(ship[0].x - 1, ship[0].y));
            }
        }
        return blockPoints;
    }

    private void blockShipRound(int direction) {
        ArrayList<Point> blockPoints = getRoundPoints(direction);
        for (Point point : blockPoints) {
            if (isValid(point.x, point.y)) {
                blockPointsMap.put(point, seaMap[point.x][point.y]);
                seaMap[point.x][point.y] = ROAD;
            }
        }
    }

    private void restoreShipRound() {
        for (Point point : blockPoints) {
            if (isValid(point.x, point.y)) {
                seaMap[point.x][point.y] = blockPointsMap.get(point);
            }
        }
        blockPointsMap.clear();
        blockPoints.clear();
    }

    // 从船当前方向去找，这个方向一定向外的
    private ArrayList<Point> getFinalPath(Point core, int direction, Point dest, int bestDir) {
        ArrayList<Point> finalPath = new ArrayList<>();
        // A*拉直的路径，船不能完整按照该路径走，只能根据方向走
        initShip(core);
        refreshShip(core, direction);
        finalPath.add(new Point(core));
        if (direction != bestDir) {
            turnDirection(direction, bestDir, dest, finalPath);
            direction = bestDir;
        }
        // 将起点周围4个点围起来，防止出现方向不一致
        blockShipRound(direction);
        ArrayList<Point> initialPath = getInitialBoatPath(ship[0], dest);
        if (initialPath == null) {
            return null;
        }
//        if (initialPath != null) {
//            return  initialPath;
//        }
        restoreShipRound();
        ArrayList<Point> straightPath = getStraightPath(initialPath);

        // 需要路径是可靠的
        if (!pathIsReliable(straightPath)) {
            // 找路之前修改地图,获取特殊点，需要将其改为障碍
            blockShipRound(direction);
            initialPath = getInitialBoatPath(ship[0], dest);
            restoreShipRound();
            ArrayList<Point> specialPointList = new ArrayList<>(specialPoint.keySet());
            changeMapinfo(specialPointList, special);
            if (initialPath == null) {
                return null;
            }
            straightPath = getStraightPath(initialPath);
            // 恢复地图上的点
            restoreMapinfo(specialPointList, special);
            specialPoint.clear(); // 清空，保证下次使用正常
        }
//        if (straightPath != null) {
//            return straightPath;
//        }

        int turnFlag = -1; // 旋转标志，0 顺，1逆，-1 zhi zou
        // 拼接最后的路径
        for (int i = 2; i < straightPath.size() - 1; i++) {
            int nextDir = getDirection(straightPath.get(i), straightPath.get(i + 1));
            if (direction == nextDir && shipBehindPathPoint(nextDir, straightPath.get(i))) {
                pushForward(direction, finalPath);
            }
            // A*开始转向的时候，船不一定能转，需要做特殊处理
            if (nextDir != direction) {
                turnFlag = getRotation(direction, nextDir);
                // 如果A*给的点不能够现在转，那么就走到能转时为止
                // 最多走两次才能转，不然就是路径寻找有问题
                int times = 0;
                // 船位置要和 i 保持一致
                while (!canTurnDir(direction, turnFlag) && times < 2) {
                    pushForward(direction, finalPath);
                    times++;
                }
                // TODO： 这个在有的时候可能会出现不知情的落后，暂时先这样吧
                if (shipBehindPathPoint(direction, straightPath.get(i))) {
                    pushForward(direction, finalPath);
                }
                turnDirection(direction, nextDir, straightPath.get(i), finalPath);
                direction = nextDir;
            }
            else {
                // 顺时针单独处理,顺时针会导致前进 1 格
                if (turnFlag == 0) {
                    if (i < straightPath.size() - 2 && ship[2].equals(straightPath.get(i + 2))) {
                        i += 1;
                    }
                    turnFlag = -1;
                }
                else if (turnFlag == 1){
                    // 在这里处理转向标记，逆时针，船需要前进一个，顺时针，线路多1格
                    turnFlag = -1;
                }
               else {
                    pushForward(direction, finalPath);
                }
            }
        }
        // 结尾处理
        finalPath.add(new Point(ship[1]));
        finalPath.add(new Point(ship[2]));
        //TODO: 按理应该不需要加这个点
        finalPath.add(straightPath.get(straightPath.size() - 1));
        return finalPath;
    }

    private ArrayList<Point> getStraightPath(ArrayList<Point> initialPath) {
        ArrayList<Point> straightPath = new ArrayList<>();

        // 创建 HashMap 来存储 initialPath 的点的索引
        HashMap<Point, Integer> pointToIndexMap = new HashMap<>();
        for (int i = 0; i < initialPath.size(); i++) {
            pointToIndexMap.put(initialPath.get(i), i);
        }

        int preDir = getDirection(initialPath.get(0), initialPath.get(1));

        Point endPoint = initialPath.get(initialPath.size() - 1);
        straightPath.add(new Point(initialPath.get(0)));
        for (int i = 1; i < initialPath.size() - 1; i++) {
            int nextDir = getDirection(initialPath.get(i), initialPath.get(i + 1));
            if (preDir != nextDir) {
                // 不一样逻辑则需要特殊处理，看能不能继续走preDir方向
                straightPath.add(new Point(initialPath.get(i))); //当前点加入
                // 能否继续加入？
                Point nextPoint = new Point(initialPath.get(i));
                while (canMove(preDir, nextDir, nextPoint, endPoint, pointToIndexMap)) {
                    straightPath.add(new Point(nextPoint)); // 将该点加入到路径去
                }

                nextPoint = new Point(straightPath.get(straightPath.size() - 1));
                // 退出的时候，需要让路径一直加入到A*得到的路径点
                while(!pointToIndexMap.containsKey(nextPoint)) {
                    getNextPoint(nextDir, nextPoint);
                    straightPath.add(new Point(nextPoint)); // 将该点加入到路径去
                }
                i = pointToIndexMap.get(nextPoint);
            }
            else {
                if (!initialPath.get(i).equals(straightPath.get(straightPath.size() - 1))) {
                    straightPath.add(new Point(initialPath.get(i)));
                }
            }
            // 更新前一个方向
            preDir = nextDir;
        }
//        // 检查
//        for (int i = 0; i < straightPath.size() - 1; i++) {
//            Point p1 = straightPath.get(i);
//            Point p2 = straightPath.get(i + 1);
//            int len =calculateDistance(p1, p2);
//            if (len != 1) {
//                printLog("straightpath 漏点了");
//            }
//        }
        // 终点特殊处理
        if (!initialPath.get(initialPath.size() - 1).equals(straightPath.get(straightPath.size() - 1))) {
            straightPath.add(new Point(initialPath.get(initialPath.size() - 1)));
        }
        return straightPath;
    }

    // 检查路径是不是有效路径
    private  boolean pathIsReliable(ArrayList<Point> straightPath) {
        boolean flag = true;
        for (int i = 1; i < straightPath.size() - 2; i++) {
            Point p1 = straightPath.get(i - 1);
            Point p2 = straightPath.get(i);
            int pathDir = getDirection(p1, p2);
            // 获取路径方向之后就需要检查，因为路径是核心点的方向，所以检查核心点右侧是否会有问题
            if (pathDir == LEFT) {
                // 左，核心点在下方
                if (!isValid(p2.x - 1, p2.y) || seaMap[p2.x - 1][p2.y] == ROAD) {
                    specialPoint.put(p2, seaMap[p2.x][p2.y]);
                    flag = false;
                }
            }
            else if (pathDir == RIGHT) {
                // 右，核心点在上方
                if (!isValid(p2.x + 1, p2.y) || seaMap[p2.x + 1][p2.y] == ROAD) {
                    specialPoint.put(p2, seaMap[p2.x][p2.y]);
                    flag = false;
                }
            }
            else if (pathDir == UP) {
                // 上，核心点在左方
                if (!isValid(p2.x, p2.y + 1) || seaMap[p2.x][p2.y + 1] == ROAD) {
                    specialPoint.put(p2, seaMap[p2.x][p2.y]);
                    flag = false;
                }
            }
            else {
                // 下，核心点在右方
                if (!isValid(p2.x, p2.y - 1) || seaMap[p2.x][p2.y - 1] == ROAD) {
                    specialPoint.put(p2, seaMap[p2.x][p2.y]);
                    flag = false;
                }
            }
        }
        return flag;
    }

    private Point offsetDestination(Point dest) {
        if (seaMap[dest.x - 1][dest.y] == ROAD) { // 上方是路
            return new Point(dest.x + 1, dest.y);
        }
        else if (seaMap[dest.x + 1][dest.y] == ROAD) { // 下方是路
            return new Point(dest.x - 1, dest.y);
        }
        else if (seaMap[dest.x][dest.y - 1] == ROAD) { // 左方是路
            return new Point(dest.x, dest.y + 1);
        }
        else if (seaMap[dest.x][dest.y + 1] == ROAD){ // 右方是路
            return new Point(dest.x, dest.y - 1);
        }
        else {
            return dest;
        }
    }

    private boolean canTurnDir(int direction, int rotation) {
        if (rotation == clockwise) {
            return canClockwiseTurn(direction);
        }
        else {
            return canCounterClockwiseTurn(direction);
        }
    }

    private int getRotation(int direction, int pathDir) {
        if (direction == LEFT) {
            return pathDir == UP ? clockwise : counterClockwise;
        }
        else if (direction == RIGHT) {
            return pathDir == DOWN ? clockwise : counterClockwise;
        }
        else if (direction == UP) {
            return pathDir == RIGHT ? clockwise : counterClockwise;
        }
        else {
            return pathDir == LEFT ? clockwise : counterClockwise;
        }
    }

    private boolean shipBehindPathPoint(int direction, Point p) {
        // 下一个点不能是墙
        if (direction == LEFT) {
            return  isValid(ship[2].x, ship[2].y - 1) && seaMap[ship[2].x][ship[2].y - 1] != ROAD &&
                    ship[2].y > p.y;
        }
        else if (direction == RIGHT) {
            return isValid(ship[2].x, ship[2].y) && seaMap[ship[2].x][ship[2].y + 1] != ROAD &&
                    ship[2].y < p.y;
        }
        else if (direction == UP) {
            return isValid(ship[2].x - 1, ship[2].y) && seaMap[ship[2].x - 1][ship[2].y] != ROAD &&
                    ship[2].x > p.x;
        }
        else {
            return isValid(ship[2].x + 1, ship[2].y ) && seaMap[ship[2].x + 1][ship[2].y] != ROAD &&
                    ship[2].x < p.x;
        }
    }

    private void getNextPoint(int nextDir, Point nextPoint) {
        if (nextDir == LEFT) {
            nextPoint.y -= 1;
        }
        else if (nextDir == RIGHT) {
            nextPoint.y += 1;
        }
        else if (nextDir == UP) {
            nextPoint.x -= 1;
        }
        else {
            nextPoint.x += 1;
        }
    }

    // TODO，暂时先这样，拉直时不考虑主航道
    private boolean canMove(int preDir, int nextDir, Point nextPoint,  Point endPoint, HashMap<Point, Integer> pointToIndexMap) {
        getNextPoint(preDir, nextPoint);
        Point next = new Point(nextPoint);
        getNextPoint(preDir, next); // 用下下个点
        return isValid(next.x, next.y) && seaMap[next.x][next.y] != ROAD
//                && costMap[nextPoint.x][nextPoint.y] != 2
                && haveIntersectedPoint(nextDir, next, endPoint, pointToIndexMap);
    }

    // 没有交接点
    private boolean haveIntersectedPoint(int nextDir, Point startPoint, Point endPoint, HashMap<Point, Integer> pointToIndexMap) {
        Point p = new Point(startPoint);
        if (nextDir == LEFT) {
            while (isValid(p.x, p.y) && p.y >= endPoint.y && seaMap[p.x][p.y] != ROAD) {
                p.y -= 1;
                if (pointToIndexMap.containsKey(p)) {
                    return true;
                }
            }
            return false;
        }
        else if (nextDir == RIGHT) {
            while (isValid(p.x, p.y) && p.y <= endPoint.y && seaMap[p.x][p.y] != ROAD) {
                p.y += 1;
                if (pointToIndexMap.containsKey(p)) {
                    return true;
                }
            }
            return false;
        }
        else if (nextDir == UP) {
            while (isValid(p.x, p.y) && p.x >= endPoint.x && seaMap[p.x][p.y] != ROAD) {
                p.x -= 1;
                if (pointToIndexMap.containsKey(p)) {
                    return true;
                }
            }
            return false;
        }
        else {
            while (isValid(p.x, p.y) && p.x <= endPoint.x && seaMap[p.x][p.y] != ROAD) {
                p.x += 1;
                if (pointToIndexMap.containsKey(p)) {
                    return true;
                }
            }
            return false;
        }
    }

    private int getDirection(Point p1, Point p2) {
        if (p1.x == p2.x) { // 水平方向移动
            if (p1.y < p2.y) {
                return RIGHT;
            }
            else {
                return LEFT;
            }
        }
        else {
            if (p1.x < p2.x) {
                return DOWN;
            }
            else {
                return UP;
            }
        }
    }

    // 曼哈顿距离小于 3 才会调用这个函数，因此不会调用太多的次
    private ArrayList<Point> getBoatPathSimply(Point core, int direction, Point dest) {
        ArrayList<Point> path = new ArrayList<>();
        // 船只设置核心点和核心点前两个点
        initShip(core);
        refreshShip(core, direction);

        // 起始点加入路径
        path.add(new Point(core));
        int times = 0;
        while (times < 3) {
            times++;
            if (ship[0].equals(dest)) {
                return path;
            }
            pushForward(direction, path);
        }
        // 因为是前置节点到达了终点，所以需要加上去
        return path;
    }

    private void initShip(Point core) {
        for (int i = 0; i < shipLen; i++) {
            ship[i] = new Point(core);
        }
    }

    // 根据当前方向获取船只核心点方向上得节点，ship[0] 对应核心点 core
    // 更新核心点上所有点的坐标
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

    private void turnDirection(int direction, int newDirection, Point dest,ArrayList<Point> path) {
        // 判断逆时针和顺时针的次数，哪个少转哪个，如果都一样优先逆时针转动
        if (turnTimes[direction][clockwise][newDirection] < turnTimes[direction][counterClockwise][newDirection]) {
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
        int nextDirection = clockwiseRotation.get(direction);
        int x = ship[2].x;
        int y = ship[2].y;
        Point tempCore = new Point(x, y);
        return canTurn(tempCore, nextDirection);
    }

    private boolean canCounterClockwiseTurn(int direction) {
        // 逆时针，核心点则沿着对角转
        int nextDirection = counterClockwiseRotation.get(direction);
        int x = ship[0].x + counterClockwiseCoordinate[direction][nextDirection][0];
        int y = ship[0].y + counterClockwiseCoordinate[direction][nextDirection][1];
        Point tempCore = new Point(x, y);
        // 判断头的两个位置即可
        return canTurn(tempCore, nextDirection);
    }

    private boolean canTurn(Point tempCore, int nextDirection) {
        // 头和尾巴都要判断
        if (nextDirection == LEFT) {
            return isValid(tempCore.x, tempCore.y - 2) && seaMap[tempCore.x][tempCore.y - 2] != ROAD &&
                    isValid(tempCore.x - 1, tempCore.y - 2) && seaMap[tempCore.x - 1][tempCore.y - 2] != ROAD &&
                    isValid(tempCore.x, tempCore.y) && seaMap[tempCore.x][tempCore.y] != ROAD &&
                    isValid(tempCore.x - 1, tempCore.y) && seaMap[tempCore.x - 1][tempCore.y] != ROAD;
        }
        else if (nextDirection == RIGHT){
            return isValid(tempCore.x, tempCore.y + 2) && seaMap[tempCore.x][tempCore.y + 2] != ROAD &&
                    isValid(tempCore.x + 1, tempCore.y + 2) && seaMap[tempCore.x + 1][tempCore.y + 2] != ROAD &&
                    isValid(tempCore.x, tempCore.y) && seaMap[tempCore.x][tempCore.y] != ROAD &&
                    isValid(tempCore.x + 1, tempCore.y) && seaMap[tempCore.x + 1][tempCore.y] != ROAD;
        }
        else if (nextDirection == UP) {
            return isValid(tempCore.x - 2, tempCore.y) && seaMap[tempCore.x - 2][tempCore.y] != ROAD &&
                    isValid(tempCore.x - 2, tempCore.y + 1) && seaMap[tempCore.x - 2][tempCore.y + 1] != ROAD &&
                    isValid(tempCore.x, tempCore.y) && seaMap[tempCore.x][tempCore.y] != ROAD &&
                    isValid(tempCore.x, tempCore.y + 1) && seaMap[tempCore.x][tempCore.y + 1] != ROAD;
        }
        else {
            return isValid(tempCore.x + 2, tempCore.y) && seaMap[tempCore.x + 2][tempCore.y] != ROAD &&
                    isValid(tempCore.x + 2, tempCore.y - 1) && seaMap[tempCore.x + 2][tempCore.y - 1] != ROAD &&
                    isValid(tempCore.x, tempCore.y) && seaMap[tempCore.x][tempCore.y] != ROAD &&
                    isValid(tempCore.x, tempCore.y - 1) && seaMap[tempCore.x][tempCore.y - 1] != ROAD;
        }
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
            if (dest != null && ship[2].x == dest.x && ship[2].y == dest.y) {
                int x = ship[0].x;
                int y = ship[0].y;
                // 以 ship 1 位置进行逆时针旋转刚好使得 x y 对齐
                getNextPoint(direction, ship[0]);

                if (canCounterClockwiseTurn(direction)) {
                    path.add(new Point(ship[0]));
                    ship[0].x = ship[0].x + counterClockwiseCoordinate[direction][tempDirection][0];;
                    ship[0].y = ship[0].y + counterClockwiseCoordinate[direction][tempDirection][1];;
                }
                else {
                    // 保持不变
                    ship[0].x = x + counterClockwiseCoordinate[direction][tempDirection][0];;
                    ship[0].y = y + counterClockwiseCoordinate[direction][tempDirection][1];;
                }
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
//        System.out.println(ship[0]);
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
        return Mapinfo.map[point.x][point.y] == MAINROAD || Mapinfo.map[point.x][point.y] == MAINBOTH;
    }

    private boolean isBoatHidePoint(Point point) {
        return Mapinfo.map[point.x][point.y] == MAINBOTH || Mapinfo.map[point.x][point.y] == MAINSEA;
    }

    // 修改地图信息以添加障碍物
    // flag 表示是船还是机器人封路，船为1，机器人为0
    private void changeMapinfo(ArrayList<Point> barriers, int flag) {
        if (flag == robot) {
            for (Point barrier : barriers) {
                if (isValid(barrier.x, barrier.y) && !isHidePoint(barrier)) {
                    landMap[barrier.x][barrier.y] = OBSTACLE;  // 标记障碍物
                }
            }
        }
        else if (flag == special){
            for (Point barrier : barriers) {
                seaMap[barrier.x][barrier.y] = ROAD;  // 标记为陆地
            }
        }
        else { // 船也是标记为陆地
            for (Point barrier : barriers) {
                if (isValid(barrier.x, barrier.y) && !isBoatHidePoint(barrier)) {
                    seaMap[barrier.x][barrier.y] = ROAD;  // 标记为陆地
                }
            }
        }
    }

    // 恢复地图信息
    private void restoreMapinfo(ArrayList<Point> barriers, int flag) {
        if (flag == robot) {
            for (Point barrier : barriers) {
                if (isValid(barrier.x, barrier.y)) {
                    landMap[barrier.x][barrier.y] = Mapinfo.map[barrier.x][barrier.y];  // 恢复为原来
                }
            }
        }
        else if (flag == special) {
            for (Point barrier : barriers) {
                Mapinfo.seaMap[barrier.x][barrier.y] = specialPoint.get(barrier);  // 恢复为原来的地图
            }
        }
        else {
            for (Point barrier : barriers) {
                if (isValid(barrier.x, barrier.y)) {
                    Mapinfo.seaMap[barrier.x][barrier.y] = Mapinfo.map[barrier.x][barrier.y];  // 恢复为原来
                }
            }
        }
    }

    private int calculateDistance(Point p1, Point p2) {
        // Example using Manhattan distance
        return Math.abs(p1.x - p2.x) + Math.abs(p1.y - p2.y);
    }

    // 陆地判断能否通过的函数
    private static boolean isAccessible(int x, int y) {
        return isValid(x, y) && Mapinfo.landMap[x][y] >= MAINBOTH;
    }

    // 海洋判断能否通过的函数
    private static boolean notAccessible(int x, int y) {
        return !isValid(x, y) || seaMap[x][y] ==  ROAD;
    }

    private static ArrayList<Point> constructPath(Pos end) {
        ArrayList<Point> path = new ArrayList<>();
        for (Pos p = end; p != null; p = p.father) {
            path.add(p.pos);
        }
        Collections.reverse(path);
        return path;
    }

    private  ArrayList<Point> getInitialBoatPath(Point p1, Point p2) {
        if (notAccessible(p1.x, p1.y) || notAccessible(p2.x, p2.y)) {
            printLog("point is impossible");
            return null;
        }

        PriorityQueue<Pos> openSet = new PriorityQueue<>(Comparator.comparingInt(Pos::f));
        Map<Point, Pos> visitedNodes = new HashMap<>();

        // 起点特殊处理
        Pos start = new Pos(p1, null, 0, calculateDistance(p1, p2));
        openSet.add(start);
        visitedNodes.put(p1, start);
        // 获取特殊点，需要将其改为障碍
        ArrayList<Point> specialPointList = new ArrayList<>(specialPoint.keySet());
        // 找路之前修改地图
        changeMapinfo(specialPointList, special);

        while (!openSet.isEmpty()) {
            Pos current = openSet.poll();
            if (current.pos.equals(p2)) {
                // 恢复地图
                restoreMapinfo(specialPointList, special);
                return constructPath(current);
            }

            for (int[] direction : directions) {
                Point newPoint = new Point(current.pos.x + direction[0], current.pos.y + direction[1]);

                if (notAccessible(newPoint.x, newPoint.y)) {
                    continue;
                }

//                int newG = current.g + 1;  // 每一步代价为1
                int newG = current.g + costMap[newPoint.x][newPoint.y];  // 每一步代价为 costMap 对应值
                if (!visitedNodes.containsKey(newPoint) || newG < visitedNodes.get(newPoint).g) {
                    int newH = calculateDistance(newPoint, p2);
                    Pos next = new Pos(newPoint, current, newG, newH);
                    openSet.add(next);
                    visitedNodes.put(newPoint, next);
                }
            }
        }
        restoreMapinfo(specialPointList, special);
        printLog("No way");
        return null;
    }
}


