package com.huawei.codecraft.way;


import com.huawei.codecraft.util.Point;
import com.huawei.codecraft.util.Twins;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * 路径接口，里面的方法上层调用，下层实现
 */
public interface Path {

    /**
     * 获取两点的路径长度
     * @param src   源点
     * @param dest  目的点
     * @return  返回路径长度，找不到返回unreachableFps
     */
    int getPathFps(Point src,Point dest);

    /**
     * 获取两点的路径点，若不为null，收尾必须为p1,p2;
     * @param src   源节点
     * @param dest  目的节点
     * @return  返回源节点到目的节点的路径，找不到返回null
     */
    ArrayList<Point> getPath(Point src,Point dest);

    /**
     * 获取拥有新障碍下的路径
     * @param pos   起点
     * @param target    目标终点
     * @param barriers  新增的障碍物，两个点，点1 对方起点位置，点2 对方下一个点位置
     * @return 返回新路径，没有则为null
     */
    ArrayList<Point> getPathWithBarrier(Point pos,Point target, HashSet<Point> barriers);

    /**
     * 获取拥有新障碍下的路径
     * @param pos   起点
     * @param target    目标终点
     * @param barriers  新增的障碍物，至少包括两个点，障碍物机器人的(pos，next)
     * @param maxLen  路径长度限制，超过这个长度不用找了直接返回null，
     * @return 返回指定长度内的新路径，没有则为null
     */
    ArrayList<Point> getPathWithBarrierWithLimit(Point pos,Point target, HashSet<Point> barriers,int maxLen);

    /**
     * 找临时避让点
     * @param pos   需要避让节点位置
     * @param leftPath    对方节点的剩余路径，避让点不能在路径上
     * @return  返回去往避让点的路径 ，没有返回null
     */
    ArrayList<Point> getHidePointPath(Point pos, List<Point> leftPath);

    /**
     * 获取海洋上的两点路径
     * @param core 轮船核心点坐标
     * @param direction 轮船起始方向
     * @param dest  轮船目的地
     * @return  轮船核心点轨迹
     */
    ArrayList<Point> getBoatPath(Point core, int direction ,Point dest);

    /**
     * 获取海洋上的两点路径
     * @param core 轮船核心点坐标
     * @param direction 轮船起始方向
     * @param dest  轮船目的地
     * @return  轮船核心点轨迹
     */
    Twins<ArrayList<Point>,Integer> getBoatPathAndFps(Point core, int direction , Point dest);
}
