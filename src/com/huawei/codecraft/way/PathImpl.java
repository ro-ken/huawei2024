package com.huawei.codecraft.way;

import com.huawei.codecraft.util.Point;
import static com.huawei.codecraft.Util.printLog;
import static com.huawei.codecraft.Const.mapWidth;
import static  com.huawei.codecraft.way.Mapinfo.map;        // 引入 Mapinfo 中的 map 变量
import static  com.huawei.codecraft.way.Mapinfo.isValid;    // 引入 Mapinfo 中的 isValid() 函数
import com.huawei.codecraft.way.Mapinfo.Terrain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

/**
 * ClassName: PathImpl
 * Package: com.huawei.codecraft.way
 * Description: 寻路的具体接口实现
 */
public class PathImpl implements Path{
    private static final int[][] directions = {{1, 0}, {-1, 0}, {0, -1}, {0, 1}};

    /**
     * 获取路径长度
     * @param p1 起点
     * @param p2 终点
     */
    @Override
    public int getPathFps(Point p1, Point p2) {
        ArrayList<Point> path = getPath(p1, p2);
        if (path == null) {
            return 10000000;
        }
        return path.size();
    }

    @Override
    public ArrayList<Point> getPath(Point p1, Point p2) {

        if (!isAccessible(p1.x, p1.y) || !isAccessible(p2.x, p2.y)) {
            printLog("point is impossible");
            return null;
        }

        boolean[][] visited = new boolean[mapWidth][mapWidth];
        Queue<Pos> queue = new LinkedList<>();

        Pos start = new Pos(p1, null); // 根据p1创建起点Pos
        Pos end = new Pos(p2, null);   // 根据p2创建终点Pos

        queue.add(start);
        visited[start.pos.x][start.pos.y] = true;

        while (!queue.isEmpty()) {
            Pos current = queue.poll();

            if (current.pos.x == end.pos.x && current.pos.y == end.pos.y) { // 找到终点
                return constructPath(current);
            }

            for (int[] direction : directions) {
                int newX = current.pos.x + direction[0];
                int newY = current.pos.y + direction[1];

                if (isAccessible(newX, newY) && !visited[newX][newY]) {
                    visited[newX][newY] = true;
                    queue.add(new Pos(new Point(newX, newY), current));
                }
            }
        }
        printLog(p1);
        printLog(p2);
        printLog("No way");
        return null;
    }

    @Override
    public ArrayList<Point> getToBerthPath(Point robotPos, Point BerthPoint) {
        Point target = offsetBerthPoint(robotPos, BerthPoint);

        return getPath(robotPos, target);
    }

    public Point offsetBerthPoint(Point robotPos, Point BerthPoint) {
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
        return isValid(x, y) && Terrain.fromValue(map[x][y]) == Terrain.LAND;
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
