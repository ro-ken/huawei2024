package com.huawei.codecraft.way;


import com.huawei.codecraft.util.Point;

import java.util.ArrayList;
import java.util.HashSet;

// 静态接口类，里面的方法上层调用，下层实现
public interface Path {

    // 获取两点的路径长度
    int getPathFps(Point p1,Point p2);

    // 获取两点的路径点，若不为null，收尾必须为p1,p2;
    ArrayList<Point> getPath(Point p1,Point p2);

    // 获取机器人去泊口最佳的路径。
    ArrayList<Point> getToBerthPath(Point robotPos,Point BerthPoint);

    /**
     * 获取拥有新障碍下的路径
     * @param pos   起点
     * @param target    目标终点
     * @param barriers  新增的障碍物，两个点，点1 对方起点位置，点2 对方下一个点位置
     * @return 返回新路径，没有则为null
     */
    ArrayList<Point> getPathWithBarrier(Point pos,Point target, HashSet<Point> barriers);

    ArrayList<Point> getHidePointPath(Point pos, ArrayList<Point> leftPath);
}
