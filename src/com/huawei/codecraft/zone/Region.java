package com.huawei.codecraft.zone;

import com.huawei.codecraft.Const;
import com.huawei.codecraft.Util;
import com.huawei.codecraft.core.Berth;
import com.huawei.codecraft.core.Good;
import com.huawei.codecraft.core.Robot;
import com.huawei.codecraft.util.Pair;
import com.huawei.codecraft.util.Point;
import com.huawei.codecraft.util.Twins;

import java.util.*;

import static com.huawei.codecraft.Const.*;

/**
 * ClassName: Region
 * Package: com.huawei.codecraft.way
 * Description: 划分地图为不同的区域，方便搜索和寻路
 */
public class Region {
    public final int id;                           // 区域 id
    public Zone zone;   // 该区域对应的zone;
    public final Set<Berth> berths;                // 区域中的泊位
    public final Set<Point> accessiblePoints;      // 区域中的可达到点
    public final Map<Integer,Integer> pathLenToNumMap;      // 计算区域点到泊口长度对应个数的map
    public final Set<Robot> assignedRobots;        // 初始静态分配给区域的机器人
    public double expLen1,expLen2 ;     // 分配一个、两个机器人的期望搬运距离
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
        this.assignedRobots = new HashSet<>();
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
    public void assignRobots(Robot robot) {
        // 互相分配
        this.assignedRobots.add(robot);
        robot.assignRegion(this);
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
                ", assignedRobots=" + assignedRobots +
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


    public boolean pickClosestTask(Robot robot) {
        // 给机器人选一个最近的任务，成功 true，失败 false，todo
        return false;
    }

    public void calcStaticValue() {
        // 计算机器人的静态价值
        // todo 后续简化，第一优先级：面积够的 > 面积不够的；第二优先级，平均距离少的 > 平均距离远的
        double t = 0;   // t为理想机器人搬运货物走的总fps
        double p = getPointProb();
        int total = totalFrame;
        for (int i = 1; i < 300; i++) {
            if (pathLenToNumMap.containsKey(i)){
                int num = pathLenToNumMap.get(i);
                t += num * i * p;
                if (t > total){ // 时间到了，不能在运
                    if (expLen1 == 0){
                        expLen1 = i;
                        total *= 2; // 2个机器人搬运距离翻倍
                    }else {
                        expLen2 = i;
                    }
                }
            }else {
                double left = total - t;    // 看还差多少fps
                if (expLen1 == 0){
                    expLen1 = i + 30 * left/total;// todo 这里参数要改
                    expLen2 = expLen1 + 100;
                }else {
                    expLen2 = i + 30 * left/total;
                }
            }
        }
    }

    public static double getPointProb() {
        // 计算每个点生成的概率
        // 计算所有空地面积
        int area = 1;
        for (Zone zone : zones) {
            area +=zone.accessPoints.size();
        }
        // 每个点的期望 = 所有物品 / 总点数
        return expGoodNum / area;
    }

    public int getClosestBerthPathFps(Point pos) {
        int min = unreachableFps;
        // 获取离该区域最近泊口的距离
        for (Berth berth : berths) {
            int t = berth.getPathFps(pos);
            if (t < min){
                min = t;
            }
        }

        return min;
    }

    public void addNewGood(Good newGood) {
        Berth berth = regionManager.getPointBelongedBerth(newGood.pos);
        if (berth == null){
            Util.printErr("addNewGood berth == null");
            return;
        }
        // 计算物品的价值
        Pair<Good> pair = berth.calcGoodValue(newGood);
        // 下面是原子操作，不能分开
        berth.domainGoodsByTime.add(newGood);
        berth.domainGoodsByValue.add(pair);
        regionGoodsByTime.add(newGood);
        regionGoodsByValue.add(pair);
    }

    public Twins<Berth,Good> getLeastGood() {
        Util.printDebug("查看拥有物品"+regionGoodsByTime);
        // 获取最久的物品
        while (!regionGoodsByTime.isEmpty()){
            Good good = regionGoodsByTime.pop();
            Berth berth = regionManager.getPointBelongedBerth(good.pos);
            berth.removeDomainGood(good);   // 能与不能都删掉
            if (berth.canCarryGood(good)){
                return new Twins<>(berth,good);
            }
        }
        return null;
    }

    public Berth getClosestBerthByPos(Point pos) {
       // 获取离该pos最近的泊口
        int min = unreachableFps;
        Berth tar = null;
        for (Berth berth : berths) {
            int t = berth.getPathFps(pos);
            if (t < min){
                min = t;
                tar = berth;
            }
        }
        return tar;
    }

    public void setZone(Zone zone) {
        this.zone = zone;
    }

    public boolean pointInMyRegion(Point pos) {
        // 这个点在我的区域中
        Berth berth = regionManager.getPointBelongedBerth(pos);
        return berths.contains(berth);
    }
}
