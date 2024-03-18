package com.huawei.codecraft.core;

import com.huawei.codecraft.Const;
import com.huawei.codecraft.Main;
import com.huawei.codecraft.Util;
import com.huawei.codecraft.util.Pair;
import com.huawei.codecraft.util.Point;
import com.huawei.codecraft.util.RobotRunMode;
import com.huawei.codecraft.util.Twins;
import com.huawei.codecraft.zone.Region;

import java.util.*;

import static com.huawei.codecraft.Const.*;

//机器人
public class Robot {
    public int id;     // 机器人编号
    public Point pos;
    public int carry;   // 是否携带货物，1：携带，0：未携带
    public int status;  // 机器人状态，0：恢复状态，1：正常运行
    public int taskStatus;    // 0：无任务，1：有任务

    public Good bookGood;  // 预定的产品
    public Berth bookBerth;  // 预定的产品
    public Route route; //
    public Point next;  // 当前帧需要移动的下一个点
    public int totalGoodNum;
    public Region region;   // 机器人属于的区域，区域初始化赋值
    public boolean changeRegionMode;    // 是否是换区域模式，这个模式下可以不拿物品朝目标区域走
    public RobotRunMode runMode = new RobotRunMode(this);

    public Robot(int id, int x, int y) {
        this.id = id;
        pos = new Point(x,y);
        route = new Route(this);
        next = pos;
    }

    public void schedule() {
        if (runMode.isHideMode()){
            hideSched();
        }else {
            if (changeRegionMode){
                changeRegionSched();
            }else {
                normalSched();
            }
        }
    }

    private void hideSched() {
        // 临时躲避模式，暂时不考虑任务
        if (arriveTarget()){
            // 到达目的地
            runMode.waitFrame ++;
        }// 否则继续
        if (runMode.tooLong() || runMode.waitFrame >=2){
            Point tar = runMode.beNormal();
            changeRoad(tar);
        }
    }

    private void changeRegionSched() {
        // 这个调度是机器人要换区域了，手里可能没有任务
        // 如果区域有物品，拿该区域的物品，切换正常模式，没有则走到底
        // 为了防止复杂计算，先设计到了区域再选择物品
        if (region.accessiblePoints.contains(this.pos)){
            Util.printDebug("changeRegionSched ： 到达本区域id:"+region.id+""+this.pos);
            changeRegionMode = false;
        }
    }

    private void normalSched() {
        if (noTask()){
            boolean picked = pickNewTask();
            if (!picked) return;    // 没有可做的任务
        }
        doTask();            // 判断该怎么走
        // 到达目的地，可能任务结束，重新分配
        if (noTask()){
//            boolean success=region.zone.reAssignRobot(this);
//            if (!success){
//                // 要换区域则，要先到区域
//                pickNewTask();
//            }
            pickNewTask();
        }
    }

    private void doTask() {
        if (!isCarry()){
            if (arriveGood()){
                // 1、如果到达了物品，捡起物品，换路线选择泊口
                if (bookGood.isExist()){
                    loadGood(); // 装货
                    changeRoad(bookBerth.pos);
                }else {
                    // 物品不存在，任务结束
                    turnOffTask();
                }
            }
        }else {
            if (arriveBerth()){
                totalGoodNum += 1;
                Main.totalGoodNum +=1;
                bookBerth.totalGoodNum += 1;
                bookBerth.region.totalGoodNum += 1;
                // 2、如果到达了泊口，卸货，任务结束
                unloadGood(); //卸货
                turnOffTask();
            }
        }
    }
    public void assignRegion(Region region){
        this.region = region;
        changeRegionMode = true;
        Berth berth=region.getClosestBerthByPos(pos);
//        Util.printDebug("机器人区域分配信息：");
//        Util.printLog(this + "->" + region);
//        Util.printLog("berth:"+berth);
        changeRoad(berth.pos);  // 先去这个点
    }

    private void unloadGood() {
        Util.printPull(id);
        carry = 0;
        bookBerth.addBerthGood(bookGood);
    }
    private void loadGood() {
        Util.printGet(id);
        carry = 1;
    }

    // 统一处理机器人移动信息
    public static void printRobotMove() {
        // 找出不能动的节点，其他节点要绕行

        // 找出有冲突的机器人
        Map<Point,Integer> pointMap = new HashMap<>();  // 所有机器人该帧经过的点,位置及个数
        for (Robot robot : robots) {
            if (robot.next.equals(robot.pos)){
                workRobots.remove(robot);
                invalidPoints.add(robot.pos);   // 不能动，无效机器人
            }
            pointMap.merge(robot.pos, 1, Integer::sum);
            pointMap.merge(robot.next, 1, Integer::sum);
        }
        boolean flag;
        do{
             flag = false;
            HashSet<Robot> clone = (HashSet<Robot>) workRobots.clone();
            for (Robot robot : clone) {
                if (pointMap.get(robot.next) == 1){
                    flag = true;    // 有节点退出
                    robot.printMove();
                    workRobots.remove(robot);
                    pointMap.merge(robot.pos, -1, Integer::sum);
                    pointMap.merge(robot.next, -1, Integer::sum);
                }
            }
        }while (flag && !workRobots.isEmpty());

        if (!workRobots.isEmpty()){
            handleConflict(new ArrayList<>(workRobots));
        }
    }

    // 处理冲突的机器人移动信息
    private static void handleConflict(ArrayList<Robot> conflict) {
//        Util.printLog("conflict:->"+conflict);
//        if (conflict.size()<2){
//            for (Robot robot : conflict) {
//                robot.printMove();
//            }
//        }
        do {
            // 找出互相冲突的机器人
            Robot robot = conflict.remove(0);
            ArrayList<Robot> clone = (ArrayList<Robot>) conflict.clone();
            ArrayList<Robot> team = new ArrayList<>();
            team.add(robot);
            for (Robot rob : clone) {
                if (MoveMaybeConflict(robot,rob)){
                    // 可能发生冲突的为一组，统一处理
                    conflict.remove(rob);
                    team.add(rob);
                }
            }
            Util.printLog("team：->"+team);
            handleTeamConflict(team);
        }while (!conflict.isEmpty());
    }

    private static boolean MoveMaybeConflict(Robot robot1, Robot robot2) {
        return robot1.pos.clacGridDis(robot2.pos) <= 3;     // 距离小于3认为可能发生冲突
    }

    private static void handleTeamConflict(ArrayList<Robot> team) {
        // 处理team团体移动冲突
        // 找到冲突的主要矛盾，2个点，其他点避让
        if (team.size()<2){
            // 错误
            for (Robot robot : team) {
                robot.changeRoadWithBarrier(robot.route.target,invalidPoints);
            }
            return;
        }
        Twins<Robot,Robot> cores = null; // 碰撞的两个核心点
        ArrayList<Robot> others = null; // 外围点
        cores = getCoreRobots(team);
        if (cores==null){
            // 错误
            Util.printErr("handleTeamConflict2");
            return;
        }
        HashSet<Point> nexts = new HashSet<>();
        for (Robot robot : team) {
            nexts.add(robot.next);
        }

        handleCoreConflict(cores,nexts);
        for (Robot robot : team) {
            if (!cores.contains(robot)){
                handleOtherConflict(cores,robot);
            }
            robot.printMove();
        }
    }

    private static void handleOtherConflict(Twins<Robot, Robot> cores, Robot robot) {
        // 处理外围机器人robot的运动问题
        // 如果当前点有人，撤一步
        // 当前点无人，下一点有人，停留
        // 当前点无人，下一点无人，前进
        HashSet<Point> points = new HashSet<>();
        points.add(cores.getObj1().pos);
        points.add(cores.getObj1().next);
        points.add(cores.getObj2().pos);
        points.add(cores.getObj2().next);
        if (points.contains(robot.pos)){
            ArrayList<Point> path = Const.path.getPathWithBarrier(robot.pos,robot.route.target, points);
            if (path != null && path.size()-1 - robot.route.leftPathLen() < 10){
                robot.route.setNewWay(path);
                Util.printLog("handleOtherConflict:"+robot+path);
                // 这里太挤了，优先换路
            }else {
                robot.moveRobotBack();// 找零时避让点
                Robot master = null;
                if (cores.getObj1().next.equals(robot.pos)){
                    master = cores.getObj1();
                }else {
                    master = cores.getObj2();
                }
                Twins<ArrayList<Point>, Integer> newPath = findTmpPoint(robot, master);
                if (newPath.getObj1() == null){
                    Util.printWarn("slave 找不到避让点");
                }else {
                    robot.gotoHidePoint(newPath.getObj1(),master);
                }
            }
        }else {
            if (points.contains(robot.next)){
                // 下一个点有人，停留
                robot.stayCurPoint();
            }else {
                // 两点都无人，照常前进
            }
        }
    }

    private void stayCurPoint() {
        next = pos;
        route.stayCurPoint();
    }

    // 处理核心冲突
    private static void handleCoreConflict(Twins<Robot,Robot> cores, HashSet<Point> nexts) {
        // 先判断优先级,低的避让
        Robot rob1 = cores.getObj1();
        Robot rob2 = cores.getObj2();
        Robot master=null,slave=null; // master 高优先级，slave 低优先级
        if (RobotRunMode.isDifferent(rob1,rob2)){
            master = RobotRunMode.selectMaster(rob1,rob2);
            slave = rob1 == master?rob2:rob1;
            Util.printLog("优先级不同：master："+master+"slave"+slave);
            handleMasterSlave(master,slave);
        }else {
            // ① 先判断是否是窄路多对一的情况，那么一要让多
            if (nexts.contains(rob1.pos) && !nexts.contains(rob2.pos)){
                handleMasterSlave(rob1,rob2);
            }else if (!nexts.contains(rob1.pos) && nexts.contains(rob2.pos)){
                handleMasterSlave(rob2,rob1);
            }else {
                // 优先级相同
                Twins<ArrayList<Point>,Integer> newPath1 = changeRoadPath(rob1,rob2);
                Twins<ArrayList<Point>,Integer> newPath2 = changeRoadPath(rob2,rob1);
                int min = Math.min(newPath1.getObj2(),newPath2.getObj2());
                if (min < 6){
                    // 认为可接受，绕路
                    if (newPath1.getObj2() == min){
                        rob1.route.setNewWay(newPath1.getObj1());
                    }else {
                        rob2.route.setNewWay(newPath2.getObj1());
                    }
                }else {
                    // 绕路太远，注意避让;
                    newPath1 = findTmpPoint(rob1,rob2);
                    newPath2 = findTmpPoint(rob2,rob1);
                    if (newPath1.getObj1() == null && newPath2.getObj1() == null){
                        Util.printErr("handleCoreConflict:都找不到避让点");
                        Util.printLog(rob1.pos+"<-位置||障碍->"+rob2.route.getLeftPath());
                    }else {
                        if (newPath1.getObj2() < newPath2.getObj2() ){
                            rob1.gotoHidePoint(newPath1.getObj1(),rob2);
                        }else {
                            rob2.gotoHidePoint(newPath2.getObj1(),rob1);
                        }
                    }
                }
            }
        }
    }

    // 处理有优先级的冲突
    private static void handleMasterSlave(Robot master, Robot slave) {
        Twins<ArrayList<Point>,Integer> newPath = changeRoadPath(slave,master);
        if (newPath.getObj2()<6){
            slave.route.setNewWay(newPath.getObj1());
        }else {
            newPath = findTmpPoint(slave,master);
            if (newPath.getObj1() == null){
                Util.printWarn("slave 找不到避让点");
            }else {
                slave.gotoHidePoint(newPath.getObj1(),master);
            }
        }
    }

    private void gotoHidePoint(ArrayList<Point> path,Robot master) {
        Util.printLog("slave:"+this.id+"master"+master.id+"hide 点："+path);
        runMode.setHideMode(master);  // 设置主从优先关系
        route.setNewWay(path);
    }

    public void updateNextPoint() {
        // 已经在下一个点了，要重新取点，否则不变
        // 2出调用，每帧中间，有新路径
        if (pos.equals(next)){
            next = route.getNextPoint();
        }
    }

    private static Twins<ArrayList<Point>, Integer> findTmpPoint(Robot rob1, Robot rob2) {
        // rob1 给rob2让路，rob1去找临时点
        ArrayList<Point> path = Const.path.getHidePointPath(rob1.pos, rob2.route.getLeftPath());
        Util.printDebug("gotoTmpPoint::pos:"+rob1.pos+"getLeftPath:"+rob2.route.getLeftPath()+" path:"+path);
        if (path == null){
            return new Twins<>(null,Const.unreachableFps);
        }
        int fps = path.size();
        return new Twins<>(path,fps);
    }

    /*
        参数1：新路径，参数2：比当前增加的距离
     */
    private static Twins<ArrayList<Point>, Integer> changeRoadPath(Robot rob1, Robot rob2) {
        // rob1 绕过 rob2 的新路径
        if (rob1.route == null){
            return new Twins<>(null,Const.unreachableFps);
        }
        HashSet<Point> barriers = new HashSet<>();
        barriers.add(rob2.pos);
        barriers.add(rob2.next);
        ArrayList<Point> path = Const.path.getPathWithBarrier(rob1.pos,rob1.route.target, barriers);
        Util.printDebug("changeRoadPath::pos:"+rob1.pos+"   target:"+rob1.route.target+"barriers:"+barriers+"   path:"+path);
        if (path == null){
            return new Twins<>(null,Const.unreachableFps);
        }
        int extraFps = path.size()-1 - rob1.route.leftPathLen();

        return new Twins<>(path,extraFps);
    }

    // 得到team有核心冲突的机器人
    private static Twins<Robot,Robot> getCoreRobots(ArrayList<Robot> team) {
        Set<Point> nexts = new HashSet<>();
        for (Robot robot : team) {
            nexts.add(robot.next);
        }
        for (Robot robot : team) {
            if (team.contains(robot.pos)){
                for (Robot robot1 : team) {
                    if (robot1==robot)continue;
                    if (robot1.pos == robot.next){
                        // 交错前行
                        return new Twins<>(robot,robot1);
                    }
                }
            }
        }


        // 返回两个核心冲突的机器人,team.size()>=2
        for (int i = 0; i < team.size() - 1; i++) {
            for (int j = i+1; j < team.size(); j++) {
                if (checkConflict(team.get(i),team.get(j))){
                    return new Twins<>(team.get(i),team.get(j));
                }
            }
        }
        return null;
    }

    private static boolean checkConflict(Robot rob1, Robot rob2) {
        // 冲突的两种情况，写一个点重合，或者对撞
        if (rob1.next.equals(rob2.next)){
            return true;
        }
        if (rob1.next.equals(rob2.pos) && rob2.next.equals(rob1.pos)){
            return true;
        }
        return false;
    }

    // 根据路线移动
    public void gotoNextPoint() {
        updateNextPoint();
        if (Const.invalidPoints.contains(next)){
            changeRoadWithBarrier(route.target,Const.invalidPoints);   // 找新路
        }

        if (next.clacGridDis(pos)>=2){
            changeRoad(route.target);
        }
    }

    private void moveRobotBack() {
        // 机器人后退一格
        next = route.getLastPoint();
    }

    // 打印自己的移动信息
    public void printMove(){
        Util.printLog("move:"+this);
        if (next == null){
            return;
        }
        if (next.x>pos.x){
            Util.printDown(id);
        }else if (next.x<pos.x){
            Util.printUp(id);
        } else if (next.y>pos.y) {
            Util.printRight(id);
        } else if (next.y<pos.y) {
            Util.printLeft(id);
        }
    }

    private boolean arriveBerth() {
        // 携带物品，并且到达目的地
        return bookBerth.inMyPlace(pos);
    }

    private boolean arriveGood() {
        // 且到达目的地
        return bookGood.pos.equals(pos);
    }

    private boolean isCarry() {
        return carry == 1;
    }

    // 到达了目的地
    private boolean arriveTarget() {
        return route.target.equals(pos);
    }

    // 当前机器人没有任务，选择一个任务
    private boolean pickNewTask() {
        Twins<Berth,Good> twins = pickBerthAndGood();
        if (twins != null){
            boolean canArrive = changeRoad(twins.getObj2().pos);
            if (canArrive){
                setBook(twins.getObj2(),twins.getObj1());
                turnOnTask();
                Util.printLog("picked task robot:"+id + ",good"+bookGood+"berth:"+bookBerth);
                return true;
            }else {
                Util.printErr("pickNewTask:pick good can't arrive!"+this+twins.getObj2());
            }
        }else {
            Util.printWarn("pickNewTask:didn't find job"+this);
        }
        return false;
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
    private Twins<Berth,Good> pickBerthAndGood() {
        // 这是经过全局调度之后的结果，
//        Twins<Berth,Good> twins = pickLeastGood();
        Twins<Berth,Good> twins = pickBestValueGood();


        return twins;
    }

    private Twins<Berth, Good> pickBestValueGood() {
        while (!region.regionGoodsByValue.isEmpty()){
            Pair<Good> pair = region.regionGoodsByValue.poll();
            Good good = pair.getKey();
            Berth berth = regionManager.globalPointToClosestBerth.get(good.pos);
            berth.removeDomainGood(pair);   // 能与不能都删掉
            if (berth.canCarryGood(good)){
                return new Twins<>(berth,good);
            }
        }
        return null;
    }

    private Twins<Berth, Good> pickLeastGood() {
        // 选择最久远的任务，防止没有任务做
        // 1、选择本区域最早的物品
        Twins<Berth,Good> twins = region.getLeastGood();
        return twins;
    }

    //预定物品
    private void setBook(Good good, Berth berth) {
        bookGood = good;
        good.setBook(this);
        bookBerth = berth;
    }

    // 选择泊口
    public Berth pickBerth() {
        int minFps = 1000000000;
        Berth target=null;
        for (Berth berth: Const.berths){
            int fps = berth.getPathFps(pos);
            if (fps<minFps){
                minFps = fps;
                target = berth;
            }
        }
        return target;
    }

    // 没有任务
    private boolean noTask() {
        return taskStatus == 0;
    }
    private void turnOnTask() {
        taskStatus = 1;
    }
    private void turnOffTask() {
        taskStatus = 0;
    }

    // 换新的路
    public boolean changeRoad(Point target){
        route.setNewWay(target);
        if (!route.target.equals(target)){
            Util.printLog(this.pos +"->"+target +":tar"+route.target);
            Util.printErr("changeRoad 找不到路");
            return false;
        }
        return true;
    }

    // 有障碍物下寻新路
    private void changeRoadWithBarrier(Point target, HashSet<Point> barriers) {
        ArrayList<Point> path = Const.path.getPathWithBarrier(pos, target, barriers);
        route.setNewWay(path);
    }

}