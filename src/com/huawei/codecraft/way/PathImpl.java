package com.huawei.codecraft.way;

import com.huawei.codecraft.Const;
import com.huawei.codecraft.util.Point;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

public class PathImpl implements Path{
    private static final int[][] directions = {{1, 0}, {-1, 0}, {0, -1}, {0, 1}};
    @Override
    public int getPathFps(Point p1, Point p2) {
        return getPath(p1, p2).size();
    }

    @Override
    public ArrayList<Point> getPath(Point p1, Point p2) {

        if (!isAccessible(p1.x, p1.y) || !isAccessible(p2.x, p2.y)) {
            return null;
        }

        boolean[][] visited = new boolean[Const.mapWidth][Const.mapWidth];
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

        return null;
    }

    private static boolean isAccessible(int x, int y) {
        return Mapinfo.isValid(x, y) && Mapinfo.map[x][y] > 0;
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
