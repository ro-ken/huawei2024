package com.huawei.codecraft.way;


import com.huawei.codecraft.util.Point;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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
     * 获取机器人去泊口最佳的路径。
     * @param robotPos  机器人位置
     * @param BerthPoint    泊口位置
     * @return 返回去往泊口的路径
     */
    ArrayList<Point> getToBerthPath(Point robotPos,Point BerthPoint);

    /**
     * 获取拥有新障碍下的路径
     * @param pos   起点
     * @param target    目标终点
     * @param barriers  新增的障碍物，两个点，点1 对方起点位置，点2 对方下一个点位置
     * @return 返回新路径，没有则为null
     */
    ArrayList<Point> getPathWithBarrier(Point pos,Point target, HashSet<Point> barriers);

    /**
     * 找临时避让点
     * @param pos   需要避让节点位置
     * @param leftPath    对方节点的剩余路径，避让点不能在路径上
     * @return  返回去往避让点的路径 ，没有返回null
     */
    ArrayList<Point> getHidePointPath(Point pos, List<Point> leftPath);

//    /**
//     * 获取全局对地图的路径
//     * @param berthPos 泊口的左上角位置
//     * @return  整张地图点对于到泊口路径map
//     */
//    Map<Point, ArrayList<Point>> getMapPathToBerth(Point berthPos);
}
