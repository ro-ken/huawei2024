package com.huawei.codecraft.util;

import java.util.Objects;

// 地图坐标点
public class Point {
    public int x;
    public int y;

    public Point() {
    }

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Point(Point pos) {
        this.x = pos.x;
        this.y = pos.y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Point)) return false;
        Point pos = (Point) o;
        return x == pos.x && y == pos.y;
    }
    public boolean equals(int x,int y) {
        return x == this.x && y == this.y;
    }

    @Override
    public String toString() {
        return "(" + x +"," + y +')';
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    // 计算两点的网格距离
    public int clacGridDis(Point point) {
        return clacGridDis(point.x,point.y);
    }

    public int clacGridDis(int x, int y) {
        return Math.abs(this.x-x)+Math.abs(this.y-y);
    }
}
