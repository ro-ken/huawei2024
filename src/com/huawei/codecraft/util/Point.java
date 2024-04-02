package com.huawei.codecraft.util;

import com.huawei.codecraft.Const;
import com.huawei.codecraft.way.Mapinfo;

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

    public static boolean isLand(Point pos) {
        // 该点位于陆地上，其不是障碍
        if (!Point.inMap(pos)){
            return false;
        }
        // 不是海洋和障碍物，都可以走
        return Const.map[pos.x][pos.y] != '*' && Const.map[pos.x][pos.y] != '#';
    }

    private static boolean inMap(Point pos) {
        // 该点在地图上
        if (pos.x<0 || pos.x >= Const.mapWidth){
            return false;
        }
        return pos.y >= 0 && pos.y < Const.mapWidth;
    }

    public static boolean isMainRoad(Point pos) {
        return Mapinfo.map[pos.x][pos.y] == 0 || Mapinfo.map[pos.x][pos.y] == 1;
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
    public int clacGridDis(Point oth) {
        return Math.abs(this.x-oth.x)+Math.abs(this.y-oth.y);
    }
}
