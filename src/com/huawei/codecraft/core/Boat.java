package com.huawei.codecraft.core;

import com.huawei.codecraft.Const;
import com.huawei.codecraft.Main;
import com.huawei.codecraft.Util;
import com.huawei.codecraft.util.BoatPath;
import com.huawei.codecraft.util.BoatStatus;
import com.huawei.codecraft.util.Point;
import com.huawei.codecraft.util.Twins;


import java.util.*;

import static com.huawei.codecraft.Const.*;
import static com.huawei.codecraft.Const.berths;
import static com.huawei.codecraft.util.BoatStatus.GO;
import static com.huawei.codecraft.util.BoatStatus.LOAD;

// 轮船
public class Boat {
    public int id;
    public int readsts;    // 状态：0正常行驶；1恢复状态；2装载状态

    public Point pos;
    public int direction;   // 机器人当前朝向
    public BoatStatus status=BoatStatus.FREE;
    public static int capacity;
    public int carry;    // 携带物品数量
    public Berth bookBerth;
    Point next;
    public int startFrame;
    public int goodSize;
    public int totalCarryValue;
    BoatRoute route;
    BoatPath myPath;
    // <轮船数，路径>，若为两艘轮船，里面路径每人一条
    public static Map<Integer,BoatPath> totalPaths = new HashMap<>();
    public static Twins<Twins<ArrayList<Berth>, Integer>,Twins<ArrayList<Berth>, Integer>> bothPath;    // todo 路径要提前排好序，后面直接用
    public boolean frameMoved;  // 船体只能输入一条指令

    public Boat(int id,Point p) {
        this.id = id;
        pos = new Point(p);
        route = new BoatRoute(this);
//        myPath = totalPaths.get(1);     // 事先走单路径
//        myPath.enable(this);
    }

    public static void handleBoatMove() {
        for (Boat boat : boats) {
            boat.printMove();
        }
    }

    public static void init() {
        initBoatPath();
    }

    private static void initBoatPath() {
        // 初始化，轮船路径,分别计算单、双路径
        BoatPath single = getSinglePath();
        totalPaths.put(1,single);
        bothPath = getBothPath();
        Util.printLog("下面为both的两条路径：");
        Util.printLog(bothPath.getObj1());
        Util.printLog(bothPath.getObj2());

        double expGood = getPeriodGoodNum(single.myPath);
        Util.printLog("单路径期望产速"+expGood);
        if (Main.assignBoatNum <=0){
            if (expGood > 50){
                Main.assignBoatNum = 2;
            }else {
                Main.assignBoatNum = 1;
            }
        }
    }

    private static Twins<Twins<ArrayList<Berth>, Integer>,Twins<ArrayList<Berth>, Integer>> getBothPath() {
        // 双轮船路径，每个轮船走自己的路径
        // 对泊口分成两个区，每个区内的泊口产量均衡一些，路径长度也不要差太多
        // 每个泊口先分给离自己最近的交货点，如果差的太多，在调整
        ArrayList<Berth> list1 = new ArrayList<>();
        ArrayList<Berth> list2 = new ArrayList<>();
        if (boatDeliveries.size() == 2){
            // 如果有两个交货点，每个泊口先分配给自己的交货点
            for (Berth berth : berths) {
                if (berth.getSeaPathFps(boatDeliveries.get(0))<berth.getSeaPathFps(boatDeliveries.get(1))){
                    list1.add(berth);
                }else {
                    list2.add(berth);
                }
                // todo 需要调整
//                adjustBerthList(list1,list2);
            }
        }else {
            // 距离来分类
            divisionBerthsByDis(list1,list2);
        }
        Twins<ArrayList<Berth>, Integer> pa1 = getSinglePathAndFpsByEnum(list1);
        Twins<ArrayList<Berth>, Integer> pa2 = getSinglePathAndFpsByEnum(list2);
        return new Twins<>(pa1,pa2);
    }

    private static void divisionBerthsByDis(ArrayList<Berth> list1, ArrayList<Berth> list2) {
        // 根据距离对泊口进行分类
        list1.clear();list2.clear();
        Berth tarBerth = berths.get(0);
        int max = 0;
        for (Berth berth : berths) {
            if (berth.neighborTotalFps > max){
                max = berth.neighborTotalFps;
                tarBerth = berth;
            }
        }
        list1.add(tarBerth);
        list2.addAll(tarBerth.neighbors);
        adjustBerthList(list1,list2);
    }

    private static double getPeriodGoodNum(ArrayList<Berth> list) {
        if (list.isEmpty()){
            return 0;
        }
        // 计算列表中泊口的周期运货量，todo 时间允许可以换暴力搜索
        Twins<ArrayList<Berth>, Integer> twins = getSinglePathAndFpsByGreedy(list);
        int period = twins.getObj2();
        double totalGoodNum = 0;
        for (Berth berth : twins.getObj1()) {
            totalGoodNum +=berth.calcPeriodGoodNum(period);
        }
        return totalGoodNum;
    }

    private static void adjustBerthList(ArrayList<Berth> list1, ArrayList<Berth> list2) {
        // 调整两个泊口列表，让每个列表尽可能均衡
        double goodNum1 = getPeriodGoodNum(list1);
        double goodNum2 = getPeriodGoodNum(list2);
        if (goodNum2 < goodNum1){
            ArrayList<Berth> tmp = list1;
            list1 = list2;
            list2 = tmp;
            double t = goodNum1;
            goodNum1 = goodNum2;
            goodNum2 = t;
        }
        while (goodNum1 < goodNum2){
            // 当调整到不能在调整时退出
            // 每次找一个最近的泊口
            Berth change = list2.get(0);
            int min = unreachableFps;
            for (Berth b2 : list2) {
                for (Berth b1 : list1) {
                    int fps = b2.getSeaPathFps(b1.core);
                    if (fps < min){
                        min = fps;
                        change = b2;
                    }
                }
            }
            // 计算转移前和转移后的周期货量大小
            double maxNum = goodNum2;
            list1.add(change);
            list2.remove(change);
            goodNum1 = getPeriodGoodNum(list1);
            goodNum2 = getPeriodGoodNum(list2);
            if (Math.max(goodNum1,goodNum2) >= maxNum){
                list1.remove(change);
                list2.add(change);
                break;  // 负优化
            }
        }
    }

    private static BoatPath getSinglePath() {
        // 获取轮船的单路径，两种方式：贪心、穷举
        BoatPath single = getSinglePathByGreedy();
        Util.printLog("贪心分配："+single);
        long t1 = System.nanoTime();
        single = getSinglePathByEnum();
        long t2 = System.nanoTime();
        Util.printLog("暴力搜索："+(t2-t1)/1000+"us"+single);
        return single;
    }

    private static BoatPath getSinglePathByGreedy() {
        Twins<ArrayList<Berth>,Integer> twins = getSinglePathAndFpsByGreedy(berths);
        return new BoatPath(twins.getObj1(),twins.getObj2());
    }

    private static Twins<ArrayList<Berth>, Integer> getSinglePathAndFpsByGreedy(ArrayList<Berth> berths) {
        Util.printDebug("getSinglePathAndFpsByGreedy"+berths);
        if (berths.isEmpty()){
            return new Twins<>(new ArrayList<>(),0);
        }
        // 贪心获取所有路径
        ArrayList<Berth> res = null;
        int minFps = unreachableFps;
        for (Point delivery : boatDeliveries) {
            // 从每个交货点开始遍历
            ArrayList<Berth> berthList = new ArrayList<>(berths);
            ArrayList<Berth> tmpList = new ArrayList<>();
            Point last = delivery;
            int totalFps = 0;
            while (!berthList.isEmpty()){
                // 每次找最短的路径
                int min = unreachableFps;
                Berth tar = berthList.get(0);
                for (Berth berth : berthList) {
                    int dis = berth.getSeaPathFps(last);
                    if (dis < min){
                        min = dis;
                        tar = berth;
                    }
                }
                totalFps += min;
                tmpList.add(tar);
                berthList.remove(tar);
                last = tar.core;
            }
            totalFps += tmpList.get(tmpList.size()-1).getClosestDeliveryFps();
            if (totalFps < minFps){
                minFps = totalFps;
                res = tmpList;
            }
        }
        return new Twins<>(res,minFps);
    }

    private static BoatPath getSinglePathByEnum() {
        Twins<ArrayList<Berth>,Integer> twins = getSinglePathAndFpsByEnum(berths);
        return new BoatPath(twins.getObj1(),twins.getObj2());
    }

    public static Twins<ArrayList<Berth>, Integer> getSinglePathAndFpsByEnum(ArrayList<Berth> berths) {
        if (berths.isEmpty()){
            return new Twins<>(new ArrayList<>(),0);
        }
        // 通过回溯求解最优路径
        ArrayList<Berth> res = null;
        int minFps = unreachableFps;
        for (Point delivery : boatDeliveries) {
            // 时间允许可第一个泊口也动态参与
            ArrayList<Berth> berthList = new ArrayList<>(berths);
            ArrayList<Berth> tmp = new ArrayList<>();
            Berth tar = berthList.get(0);
            int min = unreachableFps;
            int totalFps = 0;
            for (Berth berth : berthList) {
                // 先选择一个最近的泊口为起始点
                int fps = berth.getSeaPathFps(delivery);
                if (fps < min){
                    min = fps;
                    tar = berth;
                }
            }
            tmp.add(tar);
            berthList.remove(tar);
            totalFps += min;
            Twins<ArrayList<Berth>,Integer> backRes = backtracking(berthList, tar);
            totalFps += backRes.getObj2();
            if (totalFps<minFps){
                minFps = totalFps;
                backRes.getObj1().add(0,tar);   // 把头加上去
                res = backRes.getObj1();
            }
        }
        return new Twins<>(res,minFps);
    }

    private static Twins<ArrayList<Berth>, Integer> backtracking(ArrayList<Berth> berthList, Berth src) {
        // 计算起点为src ，终点为虚拟点，中间的为berthList的最短路径问题
        if (berthList.isEmpty()){
            int dis = src.getClosestDeliveryFps();
            return new Twins<>(new ArrayList<>(),dis);
        }

        ArrayList<Berth> list = new ArrayList<>(berthList);
        int minFps = unreachableFps;
        ArrayList<Berth> tarList = null;
        for (Berth berth : berthList) {
            int dis = src.getSeaPathFps(berth.core);
            list.remove(berth);
            Twins<ArrayList<Berth>, Integer> twins = backtracking(list, berth);
            list.add(berth);
            if (dis + twins.getObj2() < minFps){
                minFps = dis + twins.getObj2();
                tarList = twins.getObj1();
                tarList.add(0,berth);
            }
        }
        return new Twins<>(tarList,minFps);
    }

    public static Boat buySecondBoat() {
        // 买第二艘船,购买以后，两艘船根据自己的航线走
        // 确定第一艘船位置，将该区域划分给他，第二艘船自动去第二个位置
        int index = reAssignPathByBoatPos(boats.get(0));
        Twins<ArrayList<Berth>, Integer> tw;
        if (index == 1){
            tw = bothPath.getObj2();
        }else {
            tw = bothPath.getObj1();
        }
        Berth berth = tw.getObj1().get(0);
        Point pos = berth.getClosestBoatBuyPos();
        Boat boat = new Boat(1,pos);
        boat.setMyPath(tw);
        return boat;
    }

    private static int reAssignPathByBoatPos(Boat boat) {
        //
        Berth berth = null;
        if (boat.status == BoatStatus.SHIP || boat.status == LOAD){
            berth = boat.bookBerth;
        }else {
            // 该船是去虚拟点
            int min = unreachableFps;
            berth = berths.get(0);
            for (Berth ber : berths) {
                int fps = ber.getSeaPathFps(boat.route.target);
                if (fps<min){
                    min = fps;
                    berth = ber;
                }
            }
        }
        // 获取轮船路径
        Twins<ArrayList<Berth>, Integer> path = null;
        int res = -1;
        if (bothPath.getObj1().getObj1().contains(berth)){
            res = 1;
            path = bothPath.getObj1();
        }else if (bothPath.getObj2().getObj1().contains(berth)){
            res = 2;
            path = bothPath.getObj2();
        }
//        Util.printLog("path"+path );
//        Util.printLog(berth);
//        Util.printLog(bothPath.getObj1());
//        Util.printLog(bothPath.getObj2());

        boat.setMyPath(path);
        return res;
    }

    private void setMyPath(Twins<ArrayList<Berth>, Integer> path) {
        // 设置我的路径，后面要判断在什么状态
        myPath = new BoatPath(path,this);
    }

    private void printMove() {
        if (frameMoved) return;
        if (status == BoatStatus.SHIP || status == GO){
            int dis = next.clacGridDis(pos);
            Util.printLog("move:" + this);
            if (dis == 0)  {
                Util.printWarn("船位置重合！");
            }else  if (dis == 1) {
                // 1为前进
                Util.boatShip(id);
                Util.printLog("boat前进");
            }else if (dis == 2){
                if (Math.abs(next.x-pos.x)==1){
                    // 逆时针，下一个点在对角
                    Util.boatAnticlockwise(id);
                    Util.printLog("boat左转");
                }else {
                    // 顺时针
                    Util.boatClockwise(id);
                    Util.printLog("boat右转");
                }
            }else {
                Util.printErr("下一个坐标点有误");
            }
        }
    }

    public void schedule() {
        if (boat_num == 1){
            simpleSched();
        }else {
            pathSched();
        }
    }

    private void pathSched() {
        // 按照规划的路径进行调度
        if (inRecoverMode()){
            return;     // 恢复状态不能操作
        }
        if (status != BoatStatus.FREE){
            handleBoatTask();
        }
        if (status == BoatStatus.FREE && !frameMoved){
            // 没有任务 ， 且没有输出指令
            gotoBerthOrDeliveryByPath();
        }
    }



    private void simpleSched() {
        // 轮船简单调度，在各大泊口间轮转，满了卸货
        if (inRecoverMode()){
            return;     // 恢复状态不能操作
        }
        if (status != BoatStatus.FREE){
            handleBoatTask();
        }
        if (status == BoatStatus.FREE && !frameMoved){
            // 没有任务 ， 且没有输出指令
            goToBerthOrDelivery();
        }
    }

    private void gotoBerthOrDeliveryByPath() {
        // 通过实现规划的路径决定该怎么走
        Twins<Berth,Point> twins = myPath.getNextPlace();
        if (twins.getObj1() == null){
            Util.printLog("boat去交货点"+twins.getObj2());
            status = GO;
            changeRoad(twins.getObj2());
        }else {
            bookBerth = twins.getObj1();
            Util.printLog("boat下一个泊口"+bookBerth);
            status = BoatStatus.SHIP;
            changeRoad(bookBerth.pos);
        }
    }

    private void goToBerthOrDelivery() {
        Berth tarBerth = null;
        if (goodSize == 0){
            // 没有货物，选择货物最多的泊口
            tarBerth = selectMostGoodNumBerth();
        }else {
            // 有货，判断是去虚拟点还是泊口
            if (leftCapacity() >= 3){
                // 可以去泊口装货
                ArrayList<Berth> berthList = getSizeHighThanMe();
                if(berthList.isEmpty()){
                    tarBerth = selectMostGoodNumBerth();
                }else {
                    // 选择pos -> berth -> 交货点 最近的berth
                    int min = unreachableFps;
                    tarBerth = berthList.get(0);
                    for (Berth berth : berthList) {
                        int dis = Boat.getSeaPathFps(pos,direction,berth.core);
                        dis += berth.getClosestDeliveryFps();
                        if (dis < min){
                            min = dis;
                            tarBerth = berth;
                        }
                    }
                }
            }
        }

        Berth most = selectMostGoodNumBerth();
        Twins<Point, Integer> delivery = selectClosestDeliveryToBerth(most);
        if (tarBerth != null && goodSize != 0){
            int dis = getSeaPathFps(pos,direction,tarBerth.core);
            if (dis > delivery.getObj2() * 0.8){
                // 距离太长，还不如先去交货点
                tarBerth = null;
            }
        }

        if (tarBerth != null ){
            bookBerth = tarBerth;
            Util.printLog("boat下一个泊口"+bookBerth);
            status = BoatStatus.SHIP;
            changeRoad(bookBerth.pos);
        }else {
            // 去交货点
            status = GO;
            changeRoad(delivery.getObj1());
        }
    }

    private Twins<Point,Integer> selectClosestDeliveryToBerth(Berth berth) {
        // dis = pos -> delivery -> berth；选择一个最近的交货点让这段距离最小
        Point tar = boatDeliveries.get(0);
        int min = unreachableFps;
        for (Point delivery : boatDeliveries) {
            int dis = getSeaPathFps(pos,direction,delivery);
            dis += getSeaPathFps(berth.core,berth.direction,delivery);
            if (dis<min){
                min = dis;
                tar = delivery;
            }
        }
        return new Twins<>(tar,min);
    }

    public static int getSeaPathFps(Point pos, int direction, Point target) {
        if (pos.equals(target)){
            return 0;
        }

        Twins<Point,Integer> key = new Twins<>(pos,direction);
        if (seaHotPath.containsKey(key) && seaHotPath.get(key).containsKey(target)){
            return seaHotPath.get(key).get(target).getObj2();
        }else {
            // 没有先创建
            if (!seaHotPath.containsKey(key)){
                seaHotPath.put(key,new HashMap<>());
            }
            Map<Point, Twins<ArrayList<Point>, Integer>> map = seaHotPath.get(key);
            Util.printLog("海上未保存路径，先寻路：src:"+pos +"方向:"+direction+"dest:"+target);
            Twins<ArrayList<Point>, Integer> value = path.getBoatPathAndFps(pos, direction, target);
            if (value == null){
                Util.printErr("海上寻路出现问题！");
                return unreachableFps;
            }
            map.put(target,value);
            seaHotPath.put(key,map);
            return value.getObj2();
        }
    }

    private Berth selectMostGoodNumBerth() {
        int max = 0;
        Berth tar = berths.get(0);
        // 选择物品数量最多的泊口
        for (Berth berth : berths) {
            if (berth.existGoods.size()>max){
                max = berth.existGoods.size();
                tar = berth;
            }
        }
        return tar;
    }

    private ArrayList<Berth> getSizeHighThanMe() {
        //获取泊口物品数大于我的点，去这些点能装货一样，只比较远近
        ArrayList<Berth> res = new ArrayList<>();
        for (Berth berth : berths) {
            if (berth.existGoods.size() >= leftCapacity()){
                res.add(berth);
            }
        }
        return res;
    }

    public int leftCapacity() {
        return capacity-goodSize;
    }

    private void handleBoatTask() {
        if (status == BoatStatus.SHIP){
            // 驶向泊口状态
            if (isArriveBerthArea()){
                Util.printLog(this+"boat arrive："+bookBerth);
                Util.boatBerth(id);
                frameMoved = true;
                status = BoatStatus.LOAD;
            }
        }else if (status == BoatStatus.LOAD){
            if (startFrame == 0){
                startFrame = frameId;
            }
            if (isLoadFinish()){
                Util.printLog("搬运结束：startFrame"+startFrame);
                clacGoods();//结算货物
                Util.boatDept(id);
                frameMoved = true;
                status = BoatStatus.FREE; // 让轮船重新做选择
                startFrame = 0;
            }
        }else if(status == GO){
            if (isArriveDelivery()){
                resetBoat();        // 重置船
                // 需要判断是否进入最后周期
                status = BoatStatus.FREE;
            }
        }
    }

    // 换新的路
    public void changeRoad(Point target) {
        if (target.clacGridDis(pos)<=3){
            Util.printLog("距离太近，不寻路");
            return;
        }
        route.setNewWay(target);
        Util.printLog("boat 寻路："+route.way);
        if (!route.target.equals(target)) {
            Util.printLog(this.pos + "->" + target + ":tar" + route.target);
            Util.printErr("boat 找不到路");
        }
    }
    private boolean isArriveDelivery() {
        // 是否到达交货点
        return boatDeliveries.contains(pos);
    }

    private Berth selectHighValueBerth() {
        // 选择task中价值最高的泊位
        Berth target = berths.get(0);
        int maxVal = target.existValue;

        for (Berth berth : berths) {
            if (!berth.bookBoats.isEmpty()){
                continue;
            }
            if (berth.existValue>maxVal){
                maxVal = berth.existValue;
                target = berth;
            }
        }
        return target;
    }

    private boolean inRecoverMode() {
        return readsts == 1;
    }
    public void updateNextPoint() {
        // 已经在下一个点了，要重新取点，否则不变
        // 2出调用，每帧中间，有新路径
        if (pos.equals(next)) {
            next = route.getNextPoint();
        }
    }

    private boolean boatIsFull() {
        int left = capacity - goodSize;
        int loadGoods = Math.min(countGoods(),bookBerth.existGoods.size()); // 容量无限下这段时间装载量
        return left <= loadGoods;   // 实际装载量 > 轮船剩余空位
    }

    private boolean mustGotoVirtual() {
        // 时间不够，必须回虚拟点了,
        if (frameId + bookBerth.transport_time >= totalFrame){
            return true;
        }
        return false;
    }



    private void setDeadLine(Berth berth) {
        // 给这个泊口设定deadLine
//        int expSize = (int) (task.getMinT() / Good.maxSurvive * berth.staticValue.get(1).getGoodNum());

//        berth.setDeadLine(frameId + berth.transport_time + expSize/berth.loading_speed);    // 运输时间 + 装载时间
    }

    private void resetBoat() {
        goodSize = 0;
        totalCarryValue = 0;
    }

    private void changeBerthAndShip(Berth next) {
        resetBookBerth();
        if (next == null)
            return;
        shipToBerth(next);
    }


    private static ArrayList<Berth> getHighestLowestBerths(List<Berth> berthList) {
        // 得到berthList 价值最高和最低的berth；
        ArrayList<Berth> res = new ArrayList<>();
        if (berthList.isEmpty()) return res;
        Berth high = berthList.get(0);
        Berth low = berthList.get(1);
        for (Berth berth : berthList) {
            if (berth.staticValue.get(1).getGoodNum() > high.staticValue.get(1).getGoodNum()){
                high = berth;
            }
            if (berth.staticValue.get(1).getGoodNum() < low.staticValue.get(1).getGoodNum()){
                low = berth;
            }
        }
        berthList.remove(high);
        berthList.remove(low);
        res.add(high);
        res.add(low);
        return res;
    }

    private static ArrayList<Berth> getClosestTwinsBerth(List<Berth> berthList) {
        // 获取berthList中两个最近的泊口
        ArrayList<Berth> res = new ArrayList<>();
        int min = unreachableFps;
        Berth tar1 = null;
        Berth tar2 = null;
        for (int i = 0; i < berthList.size()-1; i++) {
            for (int j = i+1; j < berthList.size(); j++) {
                int dis = berthList.get(i).getPathFps(berthList.get(j).pos);
                if (dis < min){
                    min = dis;
                    tar1 = berthList.get(i);
                    tar2 = berthList.get(j);
                }
            }
        }
        if (min < unreachableFps){
            berthList.remove(tar1);
            berthList.remove(tar2);
            res.add(tar1);
            res.add(tar2);
        }
        return res;
    }


    // 是否到达了靠泊区
    private boolean isArriveBerthArea() {
        return bookBerth.boatInBerthArea.contains(pos);
    }

    private void resetBookBerth() {
        if (bookBerth != null){
            bookBerth.bookBoats.remove(this);
            bookBerth = null;
        }
    }

    private void shipToBerth(Berth berth) {
        bookBerth = berth;
        bookBerth.addBoat(this);
        readsts = 0; // 状态转换成移动
        status = BoatStatus.SHIP;
//        Util.printShip(id,bookBerth.id);
    }

    private void goToDelivery() {
        Util.printLog(this+"去交货点");
        resetBookBerth();
    }

    private int getRealLoad(){
        // 获得泊口的真实装载量
        int left = capacity - goodSize;
        int loadGoods = Math.min(countGoods(),bookBerth.existGoods.size()); // 容量无限下这段时间装载量
        int realLoad = Math.min(left,loadGoods);    // 实际装载量
        Util.printLog("船的装载："+goodSize+"/"+capacity+"，单次装载量："+realLoad + "，泊口货物："+bookBerth.existGoods.size()+"，装载时间："+(Const.frameId - startFrame));
        return realLoad;
    }

    private void clacGoods() {
        Util.printLog(this+":"+bookBerth+",结算货物：");
        int realLoad = getRealLoad();
        // 互相清算货物
        goodSize += realLoad;
        totalCarryValue += bookBerth.removeGoods(realLoad);
        for (Berth berth : Const.berths) {
            Util.printLog(berth+"堆积货物"+berth.existGoods.size()+"堆积价值"+berth.existValue);
        }
    }

    private boolean isLoadFinish() {
        int count = countGoods();
        // 没货了或装不下
        if (count >= bookBerth.existGoods.size() || count + goodSize >=capacity){
            // 装完了
            return true;
        }
        else return false;
    }

    // 计算已经装了多少货物
    private int countGoods() {
        int fps = Const.frameId - startFrame;// 当前帧也可以装，后续可以检查
        int count = fps * bookBerth.loading_speed;
        return count;
    }

    // 运输完成，没有任务
    private boolean isFree() {
        // 目标为虚拟点，且到达
        return status == BoatStatus.FREE;
    }

    @Override
    public String toString() {
        return "Boat{" +
                "id=" + id +
                ", rsts=" + readsts +
                ", pos=" + pos +
                ", dire=" + direction +
                ", status=" + status +
                ", next=" + next +
                ", target=" + route.target +
                ", carry=" + carry +
                '}';
    }
}