package com.huawei.codecraft.zone;

import com.huawei.codecraft.Util;
import com.huawei.codecraft.core.Berth;
import com.huawei.codecraft.core.Good;
import com.huawei.codecraft.core.Robot;
import com.huawei.codecraft.util.*;

import java.util.*;

import static com.huawei.codecraft.Const.*;

/**
 * ClassName: Region
 * Package: com.huawei.codecraft.way
 * Description: 划分地图为不同的区域，方便搜索和寻路
 */
public class Region {
    public final int id;                           // 区域 id
    public int points;                             // 区域拥有的点数
    public Zone zone;   // 该区域对应的zone;
    public final Set<Berth> berths;                // 区域中的泊位
    public final Set<Point> accessiblePoints;      // 该区域管理的点
    public final Map<Integer,Integer> pathLenToNumMap;      // 计算区域点到泊口长度对应个数的map
    public final Set<Robot> assignedRobots;        // 分配给区域的机器人
    public Map<Integer, RegionValue> staticValue ;     // 区域静态价值
    public  int staticAssignNum = 0;        // 静态分配给区域的机器人个数
    public final ArrayList<Region> neighborRegions;     // 距离当前region最近的region
    public PriorityQueue<Good> regionGoodsByValue = new PriorityQueue<>(new Comparator<Good>() {
        @Override
        public int compare(Good o1, Good o2) {
            // 需要被运输的货物,按照单位价值排序
            return Double.compare(o2.fpsValue,o1.fpsValue);
        }
    });
    public Deque<Good> regionGoodsByTime = new LinkedList<>();      // 需要被运输的货物,按照时间先后排序
    public TreeMap<Integer,Integer> disToNum = new TreeMap<>();     // 物品离泊口距离到数量的映射，距离泊口多少米的地方出现了几次

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
        this.pathLenToNumMap = new HashMap<>();
        this.staticValue = new HashMap<>();
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
                "机器人数"+ staticAssignNum +
                '}';
    }

    private String getPos() {
        // 获取其中一个泊口的点
        for (Berth berth : this.berths) {
            return berth.pos.toString();
        }
        return null;
    }

    // 获取该区域内两个最近的泊口,次近的，...
    public ArrayList<Berth> getClosestTwinsBerth() {
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
        Berth berth = RegionManager.pointBerthMap.get(newGood.pos);
        if (berth == null){
            Util.printErr("addNewGood berth == null");
            return;
        }
        int dis = berth.getPathFps(newGood.pos);
        disToNum.merge(dis, 1, Integer::sum);   // 统计自身区域的物品数
        newGood.setFpsValue(dis*2);



        // 下面是原子操作，不能分开
        regionGoodsByTime.add(newGood);
        regionGoodsByValue.add(newGood);
        berth.domainGoodsByTime.add(newGood);
        berth.domainGoodsByValue.add(newGood);
        for (BerthArea myArea : berth.myAreas) {
            if (newGood.fpsValue>myArea.getExpMinValue()){
                myArea.areaGoodsByTime.add(newGood);
            }

            // todo 以下记录打印测试
            if (dis<=myArea.getExpMaxStep()){
                myArea.totalGoodNum ++;
                myArea.totalGoodValue += newGood.value;
            }

        }
    }

    public Twins<Berth,Good> getLeastGood() {
        // 获取最久的物品
        while (!regionGoodsByTime.isEmpty()){
            Good good = regionGoodsByTime.pop();
            Berth berth = RegionManager.pointBerthMap.get(good.pos);
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
        for (Berth berth : this.berths) {
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

    public double removeRobotLoss() {
        // 计算删除一个机器人的单位价值损失
        int num = assignedRobots.size();
        double v1 = calcCurRegionValue(num);
        double v2 = calcCurRegionValue(num-1);
//        Util.printDebug(this+"loss R num:"+num+"v1:"+v1+"v2:"+v2);
        return v1-v2;
    }

    public double addRobotProfit() {
        // 计算得到一个机器人的单位价值增益
        int num = assignedRobots.size();
        double v1 = calcCurRegionValue(num);
        double v2 = calcCurRegionValue(num+1);
        Util.printDebug(this+"profit R num:"+num+"v1:"+v1+"v2:"+v2);
        return v2-v1;
    }

    // 这个只有在帧数够多的时候才能计算，不然可能出现偶然性
    public double calcCurRegionValue(int num) {
        if (num == 0) return 0;
//        // 计算该区域中机器人数量为num时单位时间产生的收益
        double totalTime = Good.maxSurvive;
//        // 统计totalTime时长内的总价值，按价值高低排序
        double countDis = 0;// countNum 计算的事从0到frameID的物品，我们要计算0-totalTime的物品，所有物品数要倍除
        RegionValue exp = staticValue.get(num);
        if (exp ==null){
            return 0;
        }
        // 单位价值为总价值/时间
        double totalValue = exp.getPeriodValue();
        double expValue = exp.getFpsValue();   // 单帧期望价值

        // 期望价值还要结合现有货物算综合价值
        int realTime = 0;   // 已存在
        int realValue = 0;
        // 计算区域已存在价值，只统计价值大于期望的点认为是有价值的
        for (Good good : regionGoodsByValue) {
            if (good.fpsValue > expValue){
                // 注意这两者价值计算是否统一
                Berth berth = RegionManager.pointBerthMap.get(good.pos);
                if (berth.canCarryGood(good)){
                    realTime += berth.getPathFps(good.pos) * 2;
                    realValue += good.value;
                }
            }
        }
        // 计算一个周期的平均价值 = 周期期望价值 + 现有价值 / 周期
        double totalAvg = (totalValue + realValue )/(totalTime + realTime);

//        Util.printLog("Region"+this);
        Util.printDebug("计算区域价值，num："+num+"totalAvg"+totalAvg);
        Util.printLog("countDis:"+countDis+",countNum:"+exp.getGoodNum()+",totalValue:"+totalValue);
        Util.printLog("expValue:"+expValue+",realTime:"+realTime+",realValue:"+realValue);

        return totalAvg;
    }


    public double calcToRegionDis(Region tar) {
        // 计算到其他region的价值
        // 计算最近两个泊口的距离
        int min = unreachableFps;
        for (Berth berth : this.berths) {
            Berth ber = tar.getClosestBerthByPos(berth.pos);
            int dis = berth.mapPath.get(ber.pos).size();
            if (dis < min){
                min = dis;
            }
        }
        return min;
    }

    public boolean haveHigherValueGoodThanExp(int robotNum) {
        // 本区域是否拥有指定机器人期望价值的物品
        while (!regionGoodsByValue.isEmpty()){
            Good good = regionGoodsByValue.peek();
            Berth berth = RegionManager.pointBerthMap.get(good.pos);
            if (berth.canCarryGood(good)){
                if (good.fpsValue > regionExpFpsValue(robotNum)){
                    return true;
                }else {
                    return false;
                }
            }else {
                berth.removeDomainGood(good);
            }
        }
        return false;
    }

    private double regionExpFpsValue(int robotNum) {
        // 返回本区域机器人数量为robotNum的单位期望价值
        return staticValue.get(robotNum).getFpsValue();
    }

    public Twins<Integer,Integer> getFirstHighValueLeftFps() {
        if (regionGoodsByTime.isEmpty()){
            return new Twins<>(unreachableFps,0);
        }
        // 获取第一个超过平均物品价值的时间
        // 如果区域太小，不知道exp Step，那所有都认为是高价值的，取第一个即可
        if (!staticValue.get(1).isAreaRich()){
            return new Twins<>(regionGoodsByTime.peek().leftFps(),regionGoodsByTime.size());
        }else {
            double expValue = staticValue.get(1).getFpsValue() * 0.8;   // 本区域的价值应该是要高于机器人待的区域的，所以回去条件放宽一些
            int minFps = unreachableFps;
            int goodNum = 0;
            for (Good good: regionGoodsByValue) {
                if (good.fpsValue < expValue){
                    break;  // 获取所有有价值的货物
                }
                int t = good.leftFps();
                goodNum += 1;
                if (t < minFps){
                    minFps = t;
                }
            }
            return new Twins<>(minFps,goodNum);
        }
    }

    public RegionValue getRegionValueByNum(int num) {
        if (staticValue.containsKey(num)){
            return staticValue.get(num);
        }
        // key按照 1,2,3,4 排序，取末尾的
        return staticValue.get(staticValue.size());
    }
}
