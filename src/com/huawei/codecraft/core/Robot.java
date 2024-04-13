package com.huawei.codecraft.core;

import com.huawei.codecraft.Const;
import com.huawei.codecraft.Main;
import com.huawei.codecraft.Util;
import com.huawei.codecraft.util.*;
import com.huawei.codecraft.zone.Region;
import com.huawei.codecraft.zone.RegionManager;

import java.util.*;

import static com.huawei.codecraft.Const.*;
import static com.huawei.codecraft.Main.*;

//机器人
public class Robot {
    public int id;     // 机器人编号
    public Point pos;
    public int carry;   // 是否携带货物，1：携带，0：未携带
    public int taskStatus;    // 0：无任务，1：有任务

    public Good bookGood;  // 预定的产品
    public Berth bookBerth;  // 预定的产品
    public Route route; //
    public Point next;  // 当前帧需要移动的下一个点
    public Region region;   // 机器人属于的区域，区域初始化赋值
    public boolean changeRegionMode;    // 是否是换区域模式，这个模式下可以不拿物品朝目标区域走
    public ArrayList<BerthArea> areas = new ArrayList<>();
    public int curAreaIndex;    // 当前所在区域下标
    public RobotRunMode runMode = new RobotRunMode(this);
    public boolean greedyMode = false;  // 最后阶段切换到贪心模式
    public boolean frameMoved;
    public static double leaveCoef = 0.8; // 离开系数，这个值越高，越容易发生移动，越低，越稳定，一般不超过1,如果等于0 ，只有本区域没货才离开

    public static int totalCarryValue = 0;
    public static int totalCarrySize = 0;

    /************************************************分界线***********************************************/

    public Robot(int id, Point p) {
        this.id = id;
        pos = new Point(p);
        route = new Route(this);
        next = pos;
    }
//
//    // 购买一个机器人，并对其初始化
//    public static void buyRobot() {
//        // 1、先计算能不能买
//        // 2、买了以后分配的泊口
//        // 3、对应购买点购买
//        ArrayList<BerthArea> list = Berth.assignBerthToNewRobot();
//        Robot robot ;
//        if (list.isEmpty() || list.size() > 2){
//            Util.printErr("no more Areas size="+list.size());
//            return;
//        }else {
//            // 分配了一个区域
//            Point buyPos = list.get(0).berth.pickClosestRobotBuyPos();
//            robot = Util.buyRobot(buyPos);
//            if (robot != null){
//                robot.setAreas(list);
//                Util.printLog(robot+"分配成功"+list);
//                Util.printBerthArea();
//            }
//        }
//    }


    public void schedule() {
        if (runMode.isHideMode()) {
            hideSched();
        } else {
            if (changeRegionMode) {
                changeRegionSched();
            } else {
                if (areaSched){
                    areaSched();
                }else {
                    normalSched();
                }
            }
        }
    }

    private void hideSched() {
        // 临时躲避模式，暂时不考虑任务
        if (arriveTarget()) {
            // 到达目的地
            runMode.waitFrame++;
        }// 否则继续
        if (runMode.tooLong() || runMode.waitFrame >= runMode.hideWaitTime) {
            runMode.beNormal();
        }
    }

    private void changeRegionSched() {
        // 这个调度是机器人要换区域了，手里可能没有任务
        // 如果区域有物品，拿该区域的物品，切换正常模式，没有则走到底
        // 为了防止复杂计算，先设计到了区域再选择物品
        if (region.accessiblePoints.contains(this.pos)) {
            Util.printDebug("changeRegionSched ： 到达本区域id:" + region.id + " " + this.pos);
            changeRegionMode = false;
        }
        if (Main.initFindGood && frameId>2){

            if (landHotPath.containsKey(pos)){
                // 刚开始有货就挑选一个
                Twins<Berth, Good> twins = globalGreedyAreaOnBuyPos(pos);
                setTask(twins);
                if (twins!=null){
                    Util.printDebug("机器人刚开始找到物品"+this+twins.getObj2());
                    changeRegionMode = false;
                }
            }
        }
    }

    private void areaSched() {
        handleTaskArea();            // 处理任务
        // 到达目的地，可能任务结束，重新分配
        if (noTask()) {
            Twins<Berth, Good> twins = pickNewTaskArea();
            setTask(twins);
            // todo 下面为测试记录打印
            if (twins == null){
                areas.get(curAreaIndex).waitTime ++;    // 没货，空等时间
            }
        }
    }

    private void normalSched() {
        handleTask();            // 处理任务
        // 到达目的地，可能任务结束，重新分配
        if (noTask()) {
            if (greedyMode) {
                Twins<Berth, Good> twins = pickLastGreedyTask(region.zone.berths);
                setTask(twins);
            } else {
                if (Main.dynamicRegion){
                    boolean success = region.zone.reAssignRobot(this);
                    if (!success) {
                        // 要换区域则，要先到区域
                        Twins<Berth, Good> twins = pickNewTask();
                        setTask(twins);
                    }
                }else {
                    Twins<Berth, Good> twins = pickNewTask();
                    setTask(twins);
                }
            }
        }
    }

    private Twins<Berth, Good> pickLastGreedyTask(Set<Berth> berths) {
        // 贪心选择任务，选择单次价值最高的
        double maxV = 0;
        Berth tarBer = null;
        Good tarGood = null;
        Util.printLog(this);

        for (Berth berth : berths) {
            Berth me = null;
            if (pointToBerth.containsKey(pos)){
                me = pointToBerth.get(pos);
            }

//            int r2bFps = berth.getPathFps(pos);      //时间 = 泊口到物品  + 机器人到泊口
//            double totalFps = r2bFps;

//            if (areaSched && bookBerth != berth){
//                // 限制走的距离
//                if (r2bFps >= bookBerth.getPathFps(pos)+Main.greedyMaxDis){
//                    continue;
//                }
//            }

            for (Good good : berth.domainGoodsByValue) {
                // 找出第一个满足robot的
                double fps = 0;
                if (me != null){
                    fps += me.getPathFps(good.pos);
                }else {
                    fps += berth.getPathFps(good.pos) + berth.getPathFps(pos);
                }
                if (areaSched && bookBerth != berth){
                // 限制走的距离
                if (bookBerth.getPathFps(good.pos) >= berth.getPathFps(good.pos)+Main.greedyMaxDis){
                    continue;
                }
            }
                fps *=2;
                double avgV = good.value / fps;
                if (avgV > maxV) {
                    maxV = avgV;
                    tarBer = berth;
                    tarGood = good;
                }
            }
        }
        if (tarGood != null) {
            tarBer.removeDomainGood(tarGood);
            return new Twins<>(tarBer, tarGood);
        }
        return null;
    }

    private void handleTaskArea() {
        if (noTask()) {
            return;
        }

        if (!isCarry()) {
            if (arriveGood()) {
                // 1、如果到达了物品，捡起物品，换路线选择泊口
                if (bookGood.isExist()) {
                    carryGoodToBerthArea();
                    loadGood(); // 装货
                } else {
                    // 物品不存在，任务结束
                    turnOffTask();
                }
            }else {
                // 如果
                if (route.target.equals(pos)){
                    // 目的地在原点，可能卡住了
                    turnOffTask();
                }
            }
        } else {
            if (arriveBerth()) {
                if (pos.clacGridDis(bookBerth.pos)>5){
                    Util.printErr("泊口不对....");
                }
                // 2、如果到达了泊口，卸货，任务结束
                unloadGood(); //卸货
                turnOffTask();
            }else {
                if (frameId > 14000){
                    if (bookBerth.deadLine-frameId<route.getLeftPath().size()){
                        // 当前送货节点不可用，选择新节点
                        Berth berth = getAvailAndClosestBerth(pos);
                        if (berth != null){
                            bookBerth = berth;
                            changeRoad(bookBerth.pos);
                        }
                    }
                }
                // 卡住了，重新找路
                if (route.target.equals(pos)){
                    changeRoad(bookBerth.pos);
                }
            }
        }
    }

    private void handleTask() {
        if (noTask()) {
            return;
        }

        if (!isCarry()) {
            if (arriveGood()) {
                // 1、如果到达了物品，捡起物品，换路线选择泊口
                if (bookGood.isExist()) {
                    carryGoodToBerth();
                    loadGood(); // 装货
                } else {
                    // 物品不存在，任务结束
                    turnOffTask();
                }
            }else {
                // 如果
                if (route.target.equals(pos)){
                    // 目的地在原点，可能卡住了
                    turnOffTask();
                }
            }
        } else {
            if (arriveBerth()) {
                // 2、如果到达了泊口，卸货，任务结束
                unloadGood(); //卸货
                turnOffTask();
            }else {
                // 卡住了，重新找路
                if (route.target.equals(pos)){
                    changeRoad(bookBerth.pos);
                }
            }
        }
    }

    private void carryGoodToBerthArea() {

        Util.printDebug(this+"到达物品，找泊口："+bookBerth);
        int fps = bookBerth.getPathFps(bookGood.pos);

        if (bookBerth.invalid() || frameId+fps >= bookBerth.deadLine){
            bookBerth = getAvailAndClosestBerth(pos);
            Util.printLog("当前泊口无效，新泊口："+bookBerth);
            changeRoad(bookBerth.pos);
            return;
        }

        // 如果只有一个区域，那就会这个泊口
        if (areas.size() == 2){
            // 有两个可用区域，观察是否需要换区
            BerthArea cur = areas.get(curAreaIndex);
            BerthArea next = areas.get(getNextAreaIndex());
            if (cur.areaGoodsByTime.isEmpty()){
                if (!next.areaGoodsByTime.isEmpty()){
                    // 本区域无，下区域有，换区
                    curAreaIndex = getNextAreaIndex();
                    Util.printLog("本区域无物品，下区域有物品");
                }else {
                    Util.printWarn("两个区域都无物品");
                }
            }else {
                if (next.areaGoodsByTime.isEmpty() || cur.getExpGoodNum() > next.getExpGoodNum()){
                    // 下区域无物品,或者本区域期望价值更高，不换
                }else{
                    // 两边都有物品，本区域期望价值低，对方时间到了就换
                    Good good = next.areaGoodsByTime.peek();    // 最紧急的物品
                    if (good.leftFps() <= next.getWorkTime() + next.berth.getPathFps(pos)){
                        // 换区
                        curAreaIndex = getNextAreaIndex();
                        Util.printLog("本区域价值低，下区域时间到了，换区");
                    }
                }
            }
        }

        bookBerth = areas.get(curAreaIndex).berth;
        changeRoad(bookBerth.pos);
    }

    private int getNextAreaIndex() {
        return (curAreaIndex+1)%2;
    }

    private void carryGoodToBerth() {
        if (!region.berths.contains(bookBerth)){
            if (needBackToRegion()) {
                bookBerth = region.getClosestBerthByPos(pos);
            }
        }

        // 如果能回去，就回去，不能回去，找个最近的能回去的
        if (!bookBerth.canSendToMe(bookGood.pos)) {
            // 表示进入了倒计时，切换贪婪模式
            greedyMode = true;
            Berth berth = pickClosestAndAvailBerth();
            if (berth != null) {
                bookBerth = berth;
                region = bookBerth.region;
            } else {
                Util.printErr("没有可用的Berth");
            }
        }
        changeRoad(bookBerth.pos);
    }

    private Berth pickClosestAndAvailBerth() {
        // 寻找离自己最近，且可用的泊口
        int min = unreachableFps;
        Berth tar = null;
        for (Berth berth : region.zone.berths) {
            if (berth.notFinalShip() || berth.sizeNotEnough()) {
                continue;
            }
            int dis = berth.getPathFps(pos);
            if (dis < min) {
                min = dis;
                tar = berth;
            }
        }
        return tar;
    }

    public void assignRegion(Region region) {
        // 分配时机，初始化；运送到达泊口后，选择下一个任务前
        this.region = region;
        changeRegionMode = true;
        bookBerth = region.getClosestBerthByPos(pos);   // bookBerth 一开始就要有
        changeRoad(bookBerth.pos);  // 先去这个点
    }

    private void unloadGood() {
        Util.printLog("到达泊口,卸货:"+this+bookGood);
        Util.robotPull(id);
        carry = 0;
        bookBerth.addBerthGood(bookGood);
        bookBerth.bookGoodSize--;
        // 统计运货量
        Const.totalCarrySize ++;
        Const.totalCarryValue += bookGood.value;
        bookBerth.totalCarrySize ++;
        bookBerth.totalCarryValue += bookGood.value;
        bookBerth.totalDis += bookBerth.getPathFps(bookGood.pos);
        totalCarrySize ++;
        totalCarryValue += bookGood.value;
    }

    private void loadGood() {
        Util.robotGet(id);
        carry = 1;
        bookBerth.bookGoodSize++;
    }

    // 统一处理机器人移动信息
    public static void handleRobotMove() {
        // 找出不能动的节点，其他节点要绕行

        // 找出有冲突的机器人
        Map<Point, Integer> pointMap = new HashMap<>();  // 所有机器人该帧经过的点,位置及个数
        for (Robot robot : robots) {
            if (robot.next.equals(robot.pos)) {
                workRobots.remove(robot);
                Util.printLog("stay" + robot);
                if (!Point.isMainRoad(robot.pos)){
                    invalidPoints.add(robot.pos);   // 不能动，无效机器人
                }
            }else if (Point.isMainRoad(robot.next) && Point.isMainRoad(robot.pos)){
                workRobots.remove(robot);
                robot.printMove();
            }else {
                pointMap.merge(robot.pos, 1, Integer::sum);
                pointMap.merge(robot.next, 1, Integer::sum);
            }
        }
        boolean flag;
        do {
            flag = false;
            HashSet<Robot> clone = new HashSet<>(workRobots) ;
            for (Robot robot : clone) {
                if (pointMap.get(robot.next) == 1) {
                    flag = true;    // 有节点退出
                    robot.printMove();
                    workRobots.remove(robot);
                    pointMap.merge(robot.pos, -1, Integer::sum);
                    pointMap.merge(robot.next, -1, Integer::sum);
                }
            }
        } while (flag && !workRobots.isEmpty());

        if (!workRobots.isEmpty()) {
            handleConflict(new ArrayList<>(workRobots));
        }
    }

    // 处理冲突的机器人移动信息
    private static void handleConflict(ArrayList<Robot> conflict) {

        do {
            // 找出互相冲突的机器人
            Robot robot = conflict.remove(0);
            ArrayList<Robot> clone = new ArrayList<>(conflict);
            ArrayList<Robot> team = new ArrayList<>();
            team.add(robot);
            for (Robot rob : clone) {
                if (MoveMaybeConflict(robot, rob)) {
                    // 可能发生冲突的为一组，统一处理
                    conflict.remove(rob);
                    team.add(rob);
                }
            }
            Util.printLog("team：->" + team);
            handleTeamConflict(team);
        } while (!conflict.isEmpty());
    }

    private static boolean MoveMaybeConflict(Robot robot1, Robot robot2) {
        return robot1.pos.clacGridDis(robot2.pos) <= 5;     // 距离小于3认为可能发生冲突
    }

    private static void handleTeamConflict(ArrayList<Robot> team) {
        // 处理team团体移动冲突
        // 找到冲突的主要矛盾，2个点，其他点避让
        if (team.size() < 2) {
            // 有障碍点，绕过障碍点，todo 障碍能不能让个路
            for (Robot robot : team) {
                robot.changeRoadWithBarrier(robot.route.target, invalidPoints);
            }
            return;
        }
        Twins<Robot, Robot> cores; // 碰撞的两个核心点
        cores = getCoreRobots(team);
        Util.printLog("cores:" + cores);

        handleCoreConflict(cores,team);

        handleOthersConflict(cores, team);

        if (handleConflictJudge(team)) {
            // 能走 大家都走一步
            for (Robot robot : team) {
                robot.printMove();
            }
        }else {
            // 不能走，删除主从关系，下一帧重新计算
            resetTeam(team);
        }
    }

    private static void resetTeam(ArrayList<Robot> team) {
        // 删除team的所有关系
        for (Robot robot : team) {
            robot.runMode.beNormal();
        }
    }

    private static boolean handleConflictJudge(ArrayList<Robot> team) {
        boolean canGo = true;
        // 判断是否能走
        Set<Point> tmp = new HashSet<>();
        for (Robot robot : team) {
            if (invalidPoints.contains(robot.next) || tmp.contains(robot.next)) {
                if (!robot.frameMoved){
                    canGo = false;
                }
            }
            if (!robot.frameMoved){
                tmp.add(robot.next);
            }
        }
        if (!canGo){
            invalidPoints.addAll(tmp);  // 如果不能走，把所有点都列为无效点
        }
        return canGo;
    }

    private static void handleOthersConflict(Twins<Robot, Robot> cores, ArrayList<Robot> team) {
        // 处理外围机器人robot的运动问题
        // 如果当前点有人，撤一步
        // 当前点无人，下一点有人，停留
        // 当前点无人，下一点无人，前进
        HashSet<Point> nexts = new HashSet<>();     // 下一个点位置
        HashSet<Point> curs = new HashSet<>();      // 当前位置
        Set<Robot> main = new HashSet<>();
        ArrayList<Robot> left = new ArrayList<>();
        for (Robot robot : team) {
            if (robot == cores.getObj1() || robot == cores.getObj2()){
                main.add(robot);
                curs.add(robot.pos);
                nexts.add(robot.next);
            }else {
                left.add(robot);
            }
        }
        while (!left.isEmpty()){
            Robot oth = null;
            for (Robot robot : left) {
                if (nexts.contains(robot.pos) || curs.contains(robot.next)){
                    oth = robot;
                    break;
                }
            }
            if (oth == null){
                oth = left.get(0);
                for (Robot robot : left) {
                    if (nexts.contains(robot.next)){
                        oth = robot;
                        break;
                    }
                }
            }
            // 以上为确定离冲突源最近的点
            handleOtherConflict(main,nexts,oth);
            nexts.add(oth.next);
            curs.add(oth.pos);
            main.add(oth);
            left.remove(oth);
        }
    }

    private static void handleOtherConflict(Set<Robot> main, HashSet<Point> nexts, Robot oth) {
        // 处理外围机器人robot的运动问题
        // 当前点无人，下一点有人，停留
        // 当前点无人，下一点无人，前进
        if (nexts.contains(oth.pos)) {
            if (Point.isMainRoad(oth.pos)){
                oth.frameMoved = true;
                Util.printLog(oth+"该点在主干道上，保持不动！");
                return;
            }
            HashSet<Point> barr = new HashSet<>(nexts);
            Robot master =null;
            for (Robot robot : main) {
                if (robot.next.equals(oth.pos)){
                    barr.add(robot.pos);  // 当前点也要加入到障碍里面去
                    master = robot;
                    break;
                }
            }
            ArrayList<Point> path = getPathWithBarrierWithLimit(oth,barr);
            if (path != null) {
                oth.route.setNewWay(path);
                Util.printLog("handleOtherConflict:" + oth + path);
                // 这里太挤了，优先换路
            } else {
                if (master == null){
                    Util.printErr("handleOtherConflict:master is null,main:"+main+oth);
                    return;
                }
                Twins<ArrayList<Point>, Integer> newPath = findTmpPoint(oth, master);
                if (newPath.getObj1() == null) {
                    Util.printWarn("slave 找不到避让点");
                } else {
                    oth.gotoHidePoint(newPath.getObj1(), master);
                }
            }
        } else {
            if (nexts.contains(oth.next)) {
                oth.frameMoved = true;
                Util.printLog(oth+"frameMoved，下一个位置有机器人，保持不动");

            } else {
                // 两点都无人，照常前进
                // 判断前面机器人是否选择了避让，如果避让了，就原地不动
                for (Robot robot : robots) {
                    if (robot.pos.equals(oth.next)){
                        if (robot.runMode.isHideMode()){
                            oth.frameMoved = true;
                            Util.printLog(oth+"frameMoved，前面机器人选择避让，本机器人不能前进！");
                            return;
                        }
                    }
                }
            }
        }
    }

    // 处理核心冲突
    private static void handleCoreConflict(Twins<Robot, Robot> cores, ArrayList<Robot> team) {
        // 先判断优先级,低的避让
        Robot rob1 = cores.getObj1();
        Robot rob2 = cores.getObj2();
        Robot master = null, slave = null; // master 高优先级，slave 低优先级
        if (RobotRunMode.isDifferent(rob1, rob2)) {
            master = RobotRunMode.selectMaster(rob1, rob2);
            slave = rob1 == master ? rob2 : rob1;
            Util.printLog("优先级不同：master：" + master + "slave" + slave);
            handleMasterSlave(master, slave);
        } else {
            cores=calcPriorityByUrgent(cores,team);
            // ① 先判断是否是窄路多对一的情况，那么一要让多
            if (cores != null){
                handleMasterSlave(cores.getObj1(), cores.getObj2());
            } else {
                // 优先级相同
                Twins<ArrayList<Point>, Integer> newPath1 = changeRoadPath(rob1, rob2);
                Twins<ArrayList<Point>, Integer> newPath2 = changeRoadPath(rob2, rob1);
                int min = Math.min(newPath1.getObj2(), newPath2.getObj2());
                if (min < 6) {
                    // 认为可接受，绕路
                    if (newPath1.getObj2() == min) {
                        rob1.route.setNewWay(newPath1.getObj1());
                    } else {
                        rob2.route.setNewWay(newPath2.getObj1());
                    }
                } else {
                    // 绕路太远，注意避让;
                    newPath1 = findTmpPoint(rob1, rob2);
                    newPath2 = findTmpPoint(rob2, rob1);
                    if (newPath1.getObj1() == null && newPath2.getObj1() == null) {
                        Util.printErr("handleCoreConflict:都找不到避让点");
                        Util.printLog(rob1.pos + "<-位置||障碍->" + rob2.route.getLeftPath());
                    } else {
                        if (newPath1.getObj2() < newPath2.getObj2()) {
                            rob1.gotoHidePoint(newPath1.getObj1(), rob2);
                        } else {
                            rob2.gotoHidePoint(newPath2.getObj1(), rob1);
                        }
                    }
                }
            }
        }
    }

    private static Twins<Robot, Robot> calcPriorityByUrgent(Twins<Robot, Robot> cores, ArrayList<Robot> team) {
        // 计算是否有紧急机器人，非要哪个避让，若有返回<master,slave>,若没有,都可让，返回null;
        Robot rob1 = cores.getObj1();
        Robot rob2 = cores.getObj2();
        // 先判断有无主干道，在主干道的让
        if (Point.isMainRoad(rob1.pos) || Point.isMainRoad(rob2.pos)){
            if (Point.isMainRoad(rob1.pos)){
                return new Twins<>(rob2, rob1);     // 1让
            }else {
                return new Twins<>(rob1, rob2);
            }
        }

        boolean onlyWay1 = rob1.myOnlyWay(team);
        boolean onlyWay2 = rob2.myOnlyWay(team);
        if (onlyWay1 && !onlyWay2) {
            // 2让
            Util.printLog(rob1+"不可让");
            return new Twins<>(rob1, rob2);

        } else if (!onlyWay1 && onlyWay2) {
            // 1让
            Util.printLog(rob2+"不可让");
            return new Twins<>(rob2, rob1);
        } else if (onlyWay1 && onlyWay2) {
            // 都让不了
            Util.printErr("calcPriorityByUrgent:都不能让");
            return null;
        } else {
            // 都可让
            Util.printLog("calcPriorityByUrgent:都可让");
            // 谁周围的机器人少谁让
            int num1 = rob1.getNeighborRobotNum();
            int num2 = rob2.getNeighborRobotNum();
            if (num1>num2){
                Util.printLog(rob1+"周围较挤，不让");
                return new Twins<>(rob1,rob2);
            } else if (num2 > num1) {
                Util.printLog(rob2+"周围较挤，不让");
                return new Twins<>(rob2,rob1);
            }else {
                return null;
            }
        }
    }

    private int getNeighborRobotNum() {
        // 获得周围邻居机器人数
        int count = 0;
        for (Robot robot : robots) {
            if (robot.pos.clacGridDis(pos) == 1){
                count ++ ;
            }
        }
        return count;
    }

    private boolean myOnlyWay(ArrayList<Robot> team) {
        Point[] list = new Point[]{new Point(pos.x-1,pos.y),new Point(pos.x+1,pos.y),new Point(pos.x,pos.y-1),new Point(pos.x,pos.y+1)};
        boolean onlyWay = true;
        for (Point t : list) {
            if (next.equals(t)) continue;
            if (Point.isLand(t)){
                boolean cango = true;
                for (Robot robot : team) {
                    if (invalidPoints.contains(t) || robot.pos.equals(t) || robot.next.equals(t)) {
                        cango = false;
                        break;
                    }
                }
                if (cango){
                    // 已经发现其他路了
                    onlyWay = false;
                    return onlyWay;
                }
            }
        }

        return onlyWay;
    }

    // 处理有优先级的冲突
    private static void handleMasterSlave(Robot master, Robot slave) {

        if (Point.isMainRoad(slave.pos)){
            master.printMove();
            Util.printLog(slave+"frameMoved，在主干道上，暂停一帧避让");
            slave.frameMoved = true;
            return;
        }

        Twins<ArrayList<Point>, Integer> newPath = changeRoadPath(slave, master);
        if (newPath.getObj2() < 6) {
            slave.route.setNewWay(newPath.getObj1());
        } else {
            newPath = findTmpPoint(slave, master);
            if (newPath.getObj1() == null) {
                Util.printWarn("slave 找不到避让点");
            } else {
                slave.gotoHidePoint(newPath.getObj1(), master);
            }
        }
    }

    private void gotoHidePoint(ArrayList<Point> path, Robot master) {
        Util.printLog("slave:" + this.id + "，master：" + master.id + "，hide 点：" + path);
        runMode.setHideMode(master);  // 设置主从优先关系
        route.setNewWay(path);
    }

    public void updateNextPoint() {
        // 已经在下一个点了，要重新取点，否则不变
        // 2出调用，每帧中间，有新路径
        // 快到泊口处优化找最近泊口,todo 最后几帧暴力搜索最优路线
        if (isCarry() && pos.clacGridDis(bookBerth.pos) <=4 && map[pos.x][pos.y] != 'B'){
            if (map[pos.x+1][pos.y] == 'B'){
                next = new Point(pos.x+1,pos.y);
                return;
            } else if (map[pos.x-1][pos.y] == 'B') {
                next = new Point(pos.x-1,pos.y);
                return;
            }else if (map[pos.x][pos.y+1] == 'B') {
                next = new Point(pos.x,pos.y+1);
                return;
            }else if (map[pos.x][pos.y-1] == 'B') {
                next = new Point(pos.x,pos.y-1);
                return;
            }
        }
        if (pos.equals(next)) {
            next = route.getNextPoint();
        }
    }

    private static Twins<ArrayList<Point>, Integer> findTmpPoint(Robot slave, Robot master) {
        // slave 给master让路，slave去找临时点
        List<Point> leftPath = master.route.getLeftPath();
        if (master.runMode.isHideMode()) {
            // 如果自身本身为躲避模式，那么slave还要避开master的master点
            leftPath.addAll(master.runMode.masterPath);
        }
        ArrayList<Point> path = Const.path.getHidePointPath(slave.pos, leftPath);
        Util.printDebug("gotoTmpPoint::pos:" + slave.pos + "getLeftPath:" + master.route.getLeftPath() + " path:" + path);
        if (path == null) {
            return new Twins<>(null, Const.unreachableFps);
        }
        int fps = path.size();
        return new Twins<>(path, fps);
    }

    /*
        参数1：新路径，参数2：比当前增加的距离
     */
    private static Twins<ArrayList<Point>, Integer> changeRoadPath(Robot rob1, Robot rob2) {
        // rob1 绕过 rob2 的新路径
        if (rob1.route == null) {
            return new Twins<>(null, Const.unreachableFps);
        }
        HashSet<Point> barriers = new HashSet<>();
        barriers.add(rob2.pos);
        barriers.add(rob2.next);

        ArrayList<Point> path = getPathWithBarrierWithLimit(rob1, barriers);
        Util.printDebug("changeRoadPath::pos:" + rob1.pos + "   target:" + rob1.route.target + "barriers:" + barriers + "   path:" + path);
        if (path == null) {
            return new Twins<>(null, Const.unreachableFps);
        }
        int extraFps = path.size() - 1 - rob1.route.leftPathLen();

        return new Twins<>(path, extraFps);
    }

    private static ArrayList<Point> getPathWithBarrierWithLimit(Robot robot, HashSet<Point> barriers) {
        long sta = System.nanoTime();
        int len = robot.route.leftPathLen();
        ArrayList<Point> path=null;
        if (len < 120){
            path = Const.path.getPathWithBarrierWithLimit(robot.pos, robot.route.target, barriers,len+5);
        }
        long end = System.nanoTime();
        Util.printLog("getPathWithBarrierWithLimit,len"+len+" time："+(end-sta)/1000+" us"+robot+barriers+"path:"+path);
        return path;
    }

    // 得到team有核心冲突的机器人
    private static Twins<Robot, Robot> getCoreRobots(ArrayList<Robot> team) {
        if (team.size() == 2) {
            return new Twins<>(team.get(0), team.get(1));
        }
        for (Robot robot : team) {
            for (Robot robot1 : team) {
                if (robot1 == robot) continue;
                if (robot1.pos.equals(robot.next) &&  robot1.next.equals(robot.pos)) {
                    // 交错前行
                    return new Twins<>(robot, robot1);
                }
            }
        }

        // 返回两个核心冲突的机器人,team.size()>=2
        for (int i = 0; i < team.size() - 1; i++) {
            for (int j = i + 1; j < team.size(); j++) {
                if (checkConflict(team.get(i), team.get(j))) {
                    return new Twins<>(team.get(i), team.get(j));
                }
            }
        }
        return new Twins<>(team.get(0), team.get(1));    // 没有核心冲突，说明被挡住路了，随便找一个
    }

    private static boolean checkConflict(Robot rob1, Robot rob2) {
        // 冲突的两种情况，写一个点重合，或者对撞
        if (rob1.next.equals(rob2.next)) {
            return true;
        }
        if (rob1.next.equals(rob2.pos) && rob2.next.equals(rob1.pos)) {
            return true;
        }
        return false;
    }

    // 打印自己的移动信息
    public void printMove() {
        if (frameMoved) {
            return;
        }else {
            // 确保每个机器人只打印一次
            frameMoved = true;
        }
        Util.printLog("move:" + this);
        if (next == null) {
            return;
        }
        if (!Point.isMainRoad(next)){
            if (invalidPoints.contains(next) ) {
                Util.printErr("printMove:next位置冲突！");
                return;
            }
            invalidPoints.add(next);
        }
        if (next.x > pos.x) {
            Util.robotDown(id);
        } else if (next.x < pos.x) {
            Util.robotUp(id);
        } else if (next.y > pos.y) {
            Util.robotRight(id);
        } else if (next.y < pos.y) {
            Util.robotLeft(id);
        }
    }

    private boolean arriveBerth() {
        // 携带物品，并且到达目的地
        return map[pos.x][pos.y] == 'B';
    }

    private boolean arriveGood() {
        // 且到达目的地
        return bookGood.pos.equals(pos);
    }

    public boolean isCarry() {
        return carry == 1;
    }

    // 到达了目的地
    private boolean arriveTarget() {
        return route.target.equals(pos);
    }

    private void setTask(Twins<Berth, Good> twins) {
        if (twins != null) {
            boolean canArrive = changeRoad(twins.getObj2().pos);
            if (canArrive) {
                setBook(twins.getObj2(), twins.getObj1());
                turnOnTask();
                Util.printLog("picked task robot:" + id + ",good" + bookGood + "berth:" + bookBerth);
            } else {
                Util.printErr("pickNewTask:pick good can't arrive!" + this + twins.getObj2());
            }
        } else {
            Util.printWarn("pickNewTask:didn't find job，" + this);
        }
    }

    @Override
    public String toString() {
        return "Robot{" +
                "id=" + id +
                "," + pos +
                ", next=" + next +
                ", target=" + route.target +
                '}';
    }

    // 选择物品和泊口
    private Twins<Berth, Good> pickNewTaskArea() {

        Berth berth = areas.get(curAreaIndex).berth;

        Util.printDebug(this+"机器人找新物品");
        if (berth.invalid()){
            Util.printDebug("本泊口无效");
            return globalGreedyArea();
        }

        // 先去找本泊口的货物
        for (BerthArea myArea : berth.myAreas) {
            if (!myArea.canUse) break;  // 该区域机器人未到位
            while (!myArea.areaGoodsByTime.isEmpty()){
                // 从最低的开始拿
                Good good = myArea.areaGoodsByTime.poll();

                if (!berth.canCarryGood(good)){
                    berth.removeDomainGood(good);
                    continue;
                }

                // 如果有几个时间差不多的，价值差别非常大的，都是在这
                Good peek = myArea.areaGoodsByTime.peek();
                if (peek != null && peek.fpsValue > good.value && berth.canCarryGood(peek)){
                    int min = berth.getRobotToBerthMinFps();
                    min = Math.min(min,berth.getPathFps(good.pos)*2);
                    if (min + berth.getPathFps(peek.pos) >= peek.leftFps()-2){
                        // 如果装了这个下一个就装不了了
                        myArea.areaGoodsByTime.add(good);
                        good = peek;
                    }
                }

                berth.removeDomainGood(good);

//                if (Main.limitArea){
//                    // todo 下面为测试使用，后面需要释放
//                    if (berth.getPathFps(good.pos)>berth.getAreaMaxStep()){
//                        continue;   // 不属于自己的货物不要拿
//                    }
//                }

                if (berth.canCarryGood(good)){
                    return new Twins<>(berth,good);
                }
            }
        }

        // 所有队列都没有高价值货物
        if (areas.size()>1){
            // 判断另外区域是否有物品
            Berth next = areas.get(getNextAreaIndex()).berth;
            for (BerthArea myArea : next.myAreas) {
                for (Good good : myArea.areaGoodsByTime) {
                    if (berth.canCarryGood(good)){
                        // 邻居货物不能乱删,自己拿不到，别人可能能拿

//                        if (Main.limitArea) {
//                            // todo 下面为测试使用，后面需要释放
//                            if (next.getPathFps(good.pos) > next.getAreaMaxStep()) {
//                                continue;   // 不属于自己的货物不要拿
//                            }
//                        }

                        next.removeDomainGood(good);
                        curAreaIndex = getNextAreaIndex();  // 换area
                        return new Twins<>(next,good);
                    }
                }
            }
        }

//         对面区域没货本泊口按照价值贪心
        while (!berth.domainGoodsByValue.isEmpty()) {
            Good good = berth.domainGoodsByValue.peek();
            if (!berth.canCarryGood(good)){
                berth.removeDomainGood(good);
                continue;
            }
            // 判断该货物的价值
            BerthArea area = berth.myAreas.get(berth.myAreas.size() - 1);
            if (good.fpsValue < area.getExpMinValue() * minValueCoef){
                break;    // 价值太低，懒得去
            }
            berth.removeDomainGood(good);

//            if (Main.limitArea) {
//                // todo 下面为测试使用，后面需要释放
//                if (berth.getPathFps(good.pos) > berth.getAreaMaxStep()) {
//                    continue;   // 不属于自己的货物不要拿
//                }
//            }

            if (berth.canCarryGood(good)) {
                return new Twins<>(berth, good);
            }
        }

        // 对面区域也没有货，贪心选择
        Twins<Berth, Good> twins = pickGreedyTaskInBerthNeighborArea();
        if (twins == null){
            // 都没物品
            Util.printWarn("全局找不到物品！");
        }else {
            // 还是选择运回原来的泊口
            return new Twins<>(berth,twins.getObj2());
        }
        return null;
    }

    private Twins<Berth, Good> globalGreedyAreaOnBuyPos(Point pos) {
        if (!landHotPath.containsKey(pos)){
            return null;
        }

        // 自己泊口不能用，全局贪心
        // 贪心选择任务，选择单次价值最高的
        double maxV = 0;
        Berth tarBer = null;
        Berth goodBer = null;
        Good tarGood = null;
        Util.printLog(this);
        Map<Point, List<Point>> path = landHotPath.get(pos);

        // todo 全局贪心
        for (Berth berth : region.zone.berths) {

            int index = 0;

            for (Good good : berth.domainGoodsByValue) {
                // 找出第一个满足robot的
                double fps = 0;
                fps += path.get(good.pos).size();

                fps += bookBerth.getPathFps(good.pos);
                double avgV = good.value / fps;
                if (avgV > maxV) {
                    maxV = avgV;
                    goodBer = berth;
                    tarGood = good;
                }
                if (index++>=3){
                    break;
                }
            }
        }
        if (tarGood != null) {
            goodBer.removeDomainGood(tarGood);
            return new Twins<>(bookBerth, tarGood);
        }

        return null;
    }

    private Twins<Berth, Good> globalGreedyArea() {
        // 自己泊口不能用，全局贪心
        // 贪心选择任务，选择单次价值最高的
        double maxV = 0;
        Berth tarBer = null;
        Berth goodBer = null;
        Good tarGood = null;
        Util.printLog(this);

        // todo 全局贪心

        for (Berth berth : region.zone.berths) {
            Berth me = null;
            if (pointToBerth.containsKey(pos)){
                me = pointToBerth.get(pos);
            }

            int index = 0;
            for (Good good : berth.domainGoodsByValue) {
                // 找出第一个满足robot的
                double fps = 0;
                if (me != null){
                    fps += me.getPathFps(good.pos);
                }else {
                    fps += berth.getPathFps(good.pos) + berth.getPathFps(pos);
                }
                Berth tar = berth;
                if (berth.invalid()){
                    tar = getAvailAndClosestBerth(good.pos);
                }
                fps += tar.getPathFps(good.pos);
                double avgV = good.value / fps;
                if (avgV > maxV) {
                    maxV = avgV;
                    tarBer = tar;
                    goodBer = berth;
                    tarGood = good;
                }
                if (index++>=3){
                    break;
                }
            }
        }
        if (tarGood != null) {
            goodBer.removeDomainGood(tarGood);
            return new Twins<>(tarBer, tarGood);
        }

        return null;
    }

    private Berth getAvailAndClosestBerth(Point pos) {
        // 找到一个离pos最近且有用的泊口
        Berth tar = null;
        int min = unreachableFps;
        for (Berth berth : region.zone.berths) {
            if (!berth.invalid()){
                int fps = berth.getPathFps(pos);
                if (fps <min){
                    min = fps;
                    tar = berth;
                }
            }
        }
        Util.printDebug("初次找到泊口："+tar);
        if (tar != null){
            return tar;
        }
        // 都不可用，选择一个deadline最小的
        int max = 0;
        for (Berth berth : region.zone.berths) {
            if (berth.deadLine > max){
                max = berth.deadLine;
                tar = berth;
            }
        }
        Util.printDebug("最后选择泊口："+tar);
        return tar;
    }

    // 选择物品和泊口
    private Twins<Berth, Good> pickNewTask() {
        // 这是经过全局调度之后的结果，
        // ① 先从本区域调货
        Twins<Berth, Good> twins;
        if (region.berths.contains(bookBerth)) {
            // 当前在本区域内，选择价值高的调度
            twins = pickBestValueGood();
            if (twins == null) {
                if (globalGreedy){
                    twins = pickLastGreedyTask(region.zone.berths);
                }else {
                    twins = pickGreedyTaskInNeighbor();
                }
            }
        } else {
            // 判断原区域的货物是否足够多，才回去，
            // 否则，在本区域贪婪调度，
            if (needBackToRegion()) {
                // 需要返回region
                twins = pickBestValueGood();
                if (twins == null) {
                    if (globalGreedy){
                        twins = pickLastGreedyTask(region.zone.berths);
                    }else {
                        twins = pickGreedyTaskInNeighbor();
                    }
                }
            }else {
                // 不需要回去，贪心选择
                if (globalGreedy){
                    twins = pickLastGreedyTask(region.zone.berths);
                }else {
                    twins = pickGreedyTaskInNeighbor();
                }
            }
        }
        return twins;
    }

    private boolean needBackToRegion() {
        if (region.regionGoodsByTime.isEmpty()) {
            return false;
        }
        // 当原区域价值足够，需要返回区域运货（来得及运完）
        int leftFps = unreachableFps;
        // 计算在fps内能否运完所有物品
        int needFps = region.getClosestBerthPathFps(pos);

        // 获取第一个超过平均物品价值的时间
        // 如果区域太小，不知道exp Step，那所有都认为是高价值的，取第一个即可
        RegionValue tar = region.getRegionValueByNum(region.assignedRobots.size());
        if (!tar.isAreaRich()) {
            leftFps = region.regionGoodsByTime.peek().leftFps();
            for (Good good : region.regionGoodsByTime) {
                needFps += region.getClosestBerthPathFps(good.pos) * 2;
            }
        } else {
            double expValue = tar.getSingleRobotFpsValue() * leaveCoef;   // 本区域的价值应该是要高于机器人待的区域的，所以回去条件放宽一些
            for (Good good : region.regionGoodsByValue) {
                if (good.fpsValue < expValue) {
                    break;  // 获取所有有价值的货物
                }
                int t = good.leftFps();
                needFps += region.getClosestBerthPathFps(good.pos) * 2;
                if (t < leftFps) {
                    leftFps = t;
                }
            }
        }
        return needFps >= leftFps;
    }

    private Twins<Berth, Good> pickGreedyTaskInBerthNeighborArea() {
        // 选择临近区域最有价值的货物，要求不能太远，送回来，选择最近两个区域即可,（包括本区域）
        Set<Berth> berthSet = new HashSet<>();

        berthSet.add(areas.get(0).berth);
        for (int i = 0; i < Math.min(3,areas.get(0).berth.neighbors.size()); i++) {
            berthSet.add(areas.get(0).berth.neighbors.get(i));
        }
        return pickLastGreedyTask(berthSet);
    }

    private Twins<Berth, Good> pickGreedyTaskInNeighbor() {
        // 选择临近区域最有价值的货物，要求不能太远，送回来，选择最近两个区域即可,（包括本区域）
        Set<Berth> berthSet = new HashSet<>();
        if (!region.neighborRegions.isEmpty()){
            berthSet.addAll(region.neighborRegions.get(0).berths);
        }
        if (region.neighborRegions.size() >=2){
            berthSet.addAll(region.neighborRegions.get(1).berths);
        }
        return pickLastGreedyTask(berthSet);
    }

    private Twins<Berth, Good> pickBestValueGoodArea() {
        // 如果选本区域最高价值的
        // 没有就返回null;

        while (!region.regionGoodsByValue.isEmpty()) {
            Good good = region.regionGoodsByValue.peek();
            Berth berth = RegionManager.pointBerthMap.get(good.pos);
            // 判断该货物的价值
            RegionValue regionValue = region.getRegionValueByNum(region.assignedRobots.size());
            if (regionValue.isAreaRich() && AtLeastOneRobotHereIfMore()){
                if (good.fpsValue < regionValue.getSingleRobotFpsValue() * leaveCoef){
                    return null;    // 价值太低
                }
            }
            berth.removeDomainGood(good);   // 能与不能都删掉
            if (berth.canCarryGood(good)) {
                return new Twins<>(berth, good);
            }
        }
        return null;
    }

    private Twins<Berth, Good> pickBestValueGood() {
        // 如果选本区域最高价值的
        // 没有就返回null;

        while (!region.regionGoodsByValue.isEmpty()) {
            Good good = region.regionGoodsByValue.peek();
            Berth berth = RegionManager.pointBerthMap.get(good.pos);
            // 判断该货物的价值
            RegionValue regionValue = region.getRegionValueByNum(region.assignedRobots.size());
            if (regionValue.isAreaRich() && AtLeastOneRobotHereIfMore()){
                if (good.fpsValue < regionValue.getSingleRobotFpsValue() * leaveCoef){
                    return null;    // 价值太低
                }
            }
            berth.removeDomainGood(good);   // 能与不能都删掉
            if (berth.canCarryGood(good)) {
                return new Twins<>(berth, good);
            }
        }
        return null;
    }

    private boolean AtLeastOneRobotHereIfMore() {
        // 如果该片区域有两个机器人及以上，确保有一个机器人在本地工作
        if (region.assignedRobots.size() <= 1){
            return true;
        }
        for (Robot robot : region.assignedRobots) {
            if (robot == this){
                continue;
            }
            if (region.berths.contains(robot.bookBerth)){
                // 证明该机器人在本区域
                return true;
            }
        }
        return false;
    }

    //预定物品
    private void setBook(Good good, Berth berth) {
        bookGood = good;
        bookBerth = berth;
    }

    // 没有任务
    public boolean noTask() {
        return taskStatus == 0;
    }

    private void turnOnTask() {
        taskStatus = 1;
    }

    private void turnOffTask() {
        taskStatus = 0;
    }

    // 换新的路
    public boolean changeRoad(Point target) {
        route.setNewWay(target);
        if (!route.target.equals(target) && !bookBerth.landPoints.contains(route.target)) {
            Util.printLog("机器人路径"+this.pos + "->" + target + ":tar" + route.target);
            Util.printErr("changeRoad robot 找不到路");
            return false;
        }
        return true;
    }

    // 有障碍物下寻新路
    private void changeRoadWithBarrier(Point target, HashSet<Point> barriers) {
        ArrayList<Point> path = Const.path.getPathWithBarrier(pos, target, barriers);
        route.setNewWay(path);
    }

    // 得到绕过前面机器人的路径
    private ArrayList<Point> getPathBypassRobot(Robot barrRob) {
        // 如果距离快到了，直接找就好
        List<Point> leftPath = route.getLeftPath();
        int len = leftPath.size();
        HashSet<Point> barr = new HashSet<>();
        if (len <=50){
            barr.add(barrRob.pos);
            barr.add(barrRob.next);
            // 快到终点了，直接找有没有代价不超过5的另一条路
            return path.getPathWithBarrierWithLimit(pos,route.target,barr,len+5);
        }

        // 前面有个机器人挡路了，给这个机器人绕道
        if (next.equals(barrRob.pos) && pos.equals(barrRob.next)){
            // 交错前行
            // 先判断左边能否走，(方向 pos -> next)
//            Twins<Point,Point> left
            // 左边走不了，判断右边能否走
        }
        return null;
    }

    public void pickRegion() {
        //
        Region region = RegionManager.pointRegionMap.get(pos);
        region.assignRobots(this);
        region.zone.robots.add(this);
    }

    public void setAreas(ArrayList<BerthArea> areas) {
        this.areas = areas;
        for (BerthArea area : areas) {
            area.enable(this);
        }
    }

    public void enableArea() {
        for (BerthArea area : areas) {
            area.canUse = true;
        }
    }
}