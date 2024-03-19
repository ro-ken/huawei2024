package com.huawei.codecraft.zone;

import com.huawei.codecraft.Util;
import com.huawei.codecraft.core.Berth;
import com.huawei.codecraft.core.Good;
import com.huawei.codecraft.core.Robot;
import com.huawei.codecraft.util.Pair;
import com.huawei.codecraft.util.Point;
import com.huawei.codecraft.util.RegionValue;
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
    public int points;                             // 区域拥有的点数
    public Zone zone;   // 该区域对应的zone;
    public final Set<Berth> berths;                // 区域中的泊位
    public final Set<Point> accessiblePoints;      // 该区域管理的点
    public final Map<Integer,Integer> pathLenToNumMap;      // 计算区域点到泊口长度对应个数的map
    public final Set<Robot> assignedRobots;        // 分配给区域的机器人
    public Map<Integer, RegionValue> staticValue ;     // 区域静态价值
    public  int staticAssignNum = 0;        // 静态分配给区域的机器人个数
    public final ArrayList<Region> neighborRegions;     // 距离当前region最近的region
    public PriorityQueue<Pair<Good>> regionGoodsByValue = new PriorityQueue<>();  // 需要被运输的货物,按照单位价值排序
    public Deque<Good> regionGoodsByTime = new LinkedList<>();      // 需要被运输的货物,按照时间先后排序
    public TreeMap<Integer,Integer> disToNum = new TreeMap<>();     // 物品离泊口距离到数量的映射，距离泊口多少米的地方出现了几次
    public int totalGoodNum;

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
                "hashcode" + hashCode()+
//                "zone"+zone.hashCode()+
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

    public void calcStaticValue() {
        // 计算机器人的静态价值
        // 第一优先级：面积够的 > 面积不够的；第二优先级，平均距离少的 > 平均距离远的
        double dis = 0;   // t为理想机器人搬运货物走的总fps
        double p = getPointProb()/totalFrame * Good.maxSurvive;     // Good.maxSurvive 周期内每个点产生的概率  ，计算出概率p = 0.0052;
        int total = Good.maxSurvive;   //往返fps，只有一半的时间是在去的路上
        Util.printLog(this+"管理区域大小："+accessiblePoints.size());
        Util.printLog("单位周期内每点概率："+p);
        Util.printLog(this.berths);
        int robotNum = 1;
        double totalNum = 0;
        for (int i = 1; i < 300; i++) {
            if (pathLenToNumMap.containsKey(i)){
                int num = pathLenToNumMap.get(i);
                double realNum= num * p;
                dis += i * realNum * 2;     //往返fps，只有一半的时间是在去的路上
                totalNum += realNum;
                if (dis > total){ // 时间到了，不能在运
                    totalNum -= (dis - total)/2/i;  //加多了，减回去几个
                    staticValue.put(robotNum,new RegionValue(robotNum,true,i,totalNum));
                    if (robotNum == 3){
                        break;  // 一个区域三个机器人最多了
                    }
                    robotNum ++;
                    total += total; // 2个机器人搬运距离翻倍
                }
            }else {
                while (robotNum <=3){
                    staticValue.put(robotNum,new RegionValue(robotNum,false,unreachableFps, accessiblePoints.size() * p));
                    robotNum ++;
                }
                break;
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
        Berth berth = RegionManager.pointBerthMap.get(newGood.pos);
        if (berth == null){
            Util.printErr("addNewGood berth == null");
            return;
        }
        int dis = berth.mapPath.get(newGood.pos).size();
        disToNum.merge(dis, 1, Integer::sum);   // 统计自身区域的物品数
        // 计算物品的价值
        Pair<Good> pair = berth.calcGoodValue(newGood);
        // 下面是原子操作，不能分开
        berth.domainGoodsByTime.add(newGood);
        berth.domainGoodsByValue.add(pair);
        regionGoodsByTime.add(newGood);
        regionGoodsByValue.add(pair);
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

    public double removeRobotLoss() {
        // 计算删除一个机器人的单位价值损失
        int num = assignedRobots.size();
        double v1 = calcCurRegionValue(num);
        double v2 = calcCurRegionValue(num-1);
        Util.printDebug(this+"loss R num:"+num+"v1:"+v1+"v2:"+v2);
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
//        double factor = frameId / totalTime;
//        double countNum = 0;
//        for (Map.Entry<Integer, Integer> entry : disToNum.entrySet()) {
//            double realNum = entry.getValue() / factor;
//            countDis = entry.getKey() * realNum * 2;   // 来回时间 * 2
//            countNum += realNum;
//            if (countDis > totalTime){
//                double t = (countDis - totalTime)/2;
//                countNum -= t / entry.getKey(); // 多加了，减回去
//                break;  // 时间到了
//            }
//        }
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
        for (Pair<Good> pair : regionGoodsByValue) {
            if (pair.getValue() > expValue){
                // 注意这两者价值计算是否统一
                Good good = pair.getKey();  // 后续可将无用的good去掉
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
            Pair<Good> pair = regionGoodsByValue.peek();
            Good good = pair.getKey();
            Berth berth = RegionManager.pointBerthMap.get(good.pos);
            if (berth.canCarryGood(good)){
                if (pair.getValue() > regionExpFpsValue(robotNum)){
                    return true;
                }else {
                    return false;
                }
            }else {
                berth.removeDomainGood(pair);
            }
        }
        return false;
    }

    private double regionExpFpsValue(int robotNum) {
        // 返回本区域机器人数量为robotNum的单位期望价值
        return staticValue.get(robotNum).getFpsValue();
    }
}
