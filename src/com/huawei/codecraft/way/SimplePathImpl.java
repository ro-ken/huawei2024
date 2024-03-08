package com.huawei.codecraft.way;

import com.huawei.codecraft.util.Point;

import java.util.ArrayList;

// 简单实现示例
public class SimplePathImpl implements Path{
    @Override
    public int getPathFps(Point p1, Point p2) {
        // 判空..
        int x = Math.abs(p1.x - p2.x);
        int y = Math.abs(p1.y - p2.y);
        return x+y;
    }

    @Override
    public ArrayList<Point> getPath(Point p1, Point p2) {

        return null;
    }
}
