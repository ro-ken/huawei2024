package com.huawei.codecraft.way;


import com.huawei.codecraft.util.Point;

import java.util.ArrayList;

// 静态接口类，里面的方法上层调用，下层实现
public interface Path {

    // 获取两点的路径长度
    int getPathFps(Point p1,Point p2);

    // 获取两点的路径点
    ArrayList<Point> getPath(Point p1,Point p2);

    // 获取机器人去泊口最佳的路径。
    ArrayList<Point> getToBerthPath(Point robotPos,Point BerthPoint);

}
