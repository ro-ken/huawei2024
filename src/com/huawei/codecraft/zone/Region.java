package com.huawei.codecraft.zone;

import com.huawei.codecraft.core.Berth;
import com.huawei.codecraft.core.Good;
import com.huawei.codecraft.core.Robot;
import com.huawei.codecraft.util.Pair;
import com.huawei.codecraft.util.Point;

import java.util.*;

/**
 * ClassName: Region
 * Package: com.huawei.codecraft.way
 * Description: 划分地图为不同的区域，方便搜索和寻路
 */
public class Region {
    public final int id;                           // 区域 id
    public final Set<Berth> berths;                // 区域中的泊位
    public final Set<Point> accessiblePoints;      // 区域中的可达到点
    public final Map<Integer,Integer> pathLenToNumMap;      // 计算区域点到泊口长度对应个数的map
    public final Set<Robot> staticAssignRobots;        // 初始静态分配给区域的机器人
    public int staticValue = 0;     // region的静态价值
    public  int staticAssignNum = 0;        // 静态分配给区域的机器人个数
    public final Set<Robot> realAssignRobots;        // 真实分配给区域的机器人
    public final ArrayList<Region> neighborRegions;     // 距离当前region最近的region
    public PriorityQueue<Pair<Good>> regionGoodsByValue = new PriorityQueue<>();  // 需要被运输的货物,按照单位价值排序
    public Deque<Good> regionGoodsByTime = new LinkedList<>();      // 需要被运输的货物,按照时间先后排序

    /**
     * 构造函数
     *
     */
    public Region(int id) {
        this.id = id;
        this.berths = new HashSet<>();
        this.accessiblePoints = new HashSet<>();
        this.staticAssignRobots = new HashSet<>();
        this.neighborRegions = new ArrayList<>();
        this.realAssignRobots = new HashSet<>();
        this.pathLenToNumMap = new HashMap<>();
    }

    public void addBerth(Berth berth) {
        this.berths.add(berth);
        berth.assignRegion(this);   // 反向引用
    }

    public void addAccessiblePoint(Point point) {
        this.accessiblePoints.add(point);
    }

    public int getId() {
        return id;
    }

    public Set<Berth> getBerths() {
        return berths;
    }

    public Set<Point> getAccessiblePoints() {
        return accessiblePoints;
    }

    public ArrayList<Region> getNeighborRegions() {
        return neighborRegions;
    }

    public Map<Integer, Integer> getPathLenToNumMap() {
        return pathLenToNumMap;
    }

    @Override
    public String toString() {
        return "Region{" +
                "id=" + id +
                ", berths=" + berths +
                ", assignedRobots=" + staticAssignRobots +
                '}';
    }

    // 获取该区域内两个最近的泊口,次近的，...
    public ArrayList<Berth> getCloestTwinsBerth() {
        if (berths.size()<2){
            return new ArrayList<>();
        }
        if (berths.size() == 2){
            return new ArrayList<>(berths);
        }
        ArrayList<Berth> clone = new ArrayList<>(berths);
        ArrayList<Berth> res = new ArrayList<>();
        do {
            ArrayList<Berth> tmp = new ArrayList<>(clone);
            Berth b0 = clone.get(0);
            Berth b1 = clone.get(1);
            int min = b0.getPathFps(b1.pos);
            for (int i = 0; i < tmp.size()-1; i++) {
                for (int j = i+1; j < tmp.size(); j++) {
                    if (tmp.get(i).getPathFps(tmp.get(j).pos) < min){
                        min = tmp.get(i).getPathFps(tmp.get(j).pos);
                        b0 = tmp.get(i) ; b1 = tmp.get(j);
                    }
                }
            }
            res.add(b0);res.add(b1);
            clone.remove(b0);clone.remove(b1);
        }while (clone.size()>=2);

        return res;
    }

    // 找出该区域下点对应的berth
    public Berth getPointToBerth(Point pos) {
        for (Berth berth : berths) {
            if (berth.mapPath.containsKey(pos)){
                return berth;
            }
        }
        return null;
    }

    public boolean pickClosestTask(Robot robot) {
        // 给机器人选一个最近的任务，成功 true，失败 false，todo
        return false;
    }

    public void calcStaticValue() {
        // 计算机器人的静态价值


    }
}
