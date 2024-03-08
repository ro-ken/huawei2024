package com.huawei.codecraft.way;

import com.huawei.codecraft.util.Point;

import java.util.ArrayList;

// 简单实现示例
public class SimplePathImpl implements Path{
    @Override
    public int getPathFps(Point p1, Point p2) {
        int x = Math.abs(p1.x - p2.x);
        int y = Math.abs(p1.y - p2.y);
        return x+y;
    }

    @Override
    public ArrayList<Point> getPath(Point p1, Point p2) {
        ArrayList<Point> res = new ArrayList<>();
        int len = Math.abs(p2.x-p1.x);
        int t = p2.x-p1.x>0 ? 1:-1;
        for (int i = 0; i <= len; i++) {
            Point p = new Point(p1.x+i*t,p1.y);
            res.add(p);
        }
        len = Math.abs(p2.y-p1.y);
        t = p2.y-p1.y>0 ? 1:-1;

        for (int i = 1; i <= len; i++) {
            Point p = new Point(p2.x,p1.y+i*t);
            res.add(p);
        }

        return res;
    }
}
