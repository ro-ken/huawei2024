package com.huawei.codecraft.core;

import com.huawei.codecraft.Const;
import com.huawei.codecraft.Util;
import com.huawei.codecraft.util.Point;
import com.huawei.codecraft.util.Twins;

import java.util.*;

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
    public boolean hideMode = false;    // 是否是避让模式， 到临时点停2帧，等对方过了在走
    public static HashSet<Robot> frameRobotMove = new HashSet<>();  // 所有机器人移动信息

    public Robot(int id, int x, int y) {
        this.id = id;
        pos = new Point(x,y);
        route = new Route(pos,this);
        next = pos;
    }

    public void schedule() {
        if (noTask()){
            boolean picked = pickNewTask();
            if (!picked) return;    // 没有可做的任务
        }
        doTask();
        // 判断该怎么走
    }

    private void doTask() {
        if (arriveTarget()){
            convertTask();
        }
        // 到达目的地，可能任务结束，重新分配
        if (noTask()){
            pickNewTask();
        }

    }

    // 统一处理机器人移动信息
    public static void printRobotMove() {
        // 找出有冲突的机器人
        Map<Point,Integer> pointMap = new HashMap<>();  // 所有机器人该帧经过的点,位置及个数
        for (Robot robot : frameRobotMove) {
            pointMap.merge(robot.pos, 1, Integer::sum);
        }
        boolean flag;
        do{
             flag = false;
            HashSet<Robot> clone = (HashSet<Robot>) frameRobotMove.clone();
            for (Robot robot : clone) {
                if (pointMap.get(robot.next) == 1){
                    flag = true;    // 有节点退出
                    robot.printMove();
                    frameRobotMove.remove(robot);
                    pointMap.merge(robot.pos, -1, Integer::sum);
                    pointMap.merge(robot.next, -1, Integer::sum);
                }
            }
        }while (flag && !frameRobotMove.isEmpty());

        if (!frameRobotMove.isEmpty()){
            handleConflict(new ArrayList<>(frameRobotMove));
        }
    }

    // 处理冲突的机器人移动信息
    private static void handleConflict(ArrayList<Robot> conflict) {
        if (conflict.size()<2){
            for (Robot robot : conflict) {
                robot.printMove();
            }
        }
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
            Util.printErr("handleTeamConflict");
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
        handleCoreConflict(cores);
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
        Set<Point> points = new HashSet<>();
        points.add(cores.getObj1().pos);
        points.add(cores.getObj1().next);
        points.add(cores.getObj2().pos);
        points.add(cores.getObj2().next);
        if (points.contains(robot.pos)){
            robot.moveRobotBack();
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


    private static void handleCoreConflict(Twins<Robot,Robot> cores) {
        Robot rob1 = cores.getObj1();
        Robot rob2 = cores.getObj2();
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
            newPath1 = gotoTmpPoint(rob1,rob2);
            newPath2 = gotoTmpPoint(rob1,rob2);
            if (newPath1.getObj2() < newPath2.getObj2() ){
                rob1.gotoHidePoint(newPath1.getObj1());
            }else {
                rob2.gotoHidePoint(newPath2.getObj1());
            }
        }
    }

    private void gotoHidePoint(ArrayList<Point> path) {
        // 去临时点
        hideMode = true;
        route.setNewWay(path);
    }

    public void updateNextPoint() {
        // 已经在下一个点了，要重新取点，否则不变
        // 2出调用，每帧中间，有新路径
        if (pos.equals(next)){
            next = route.getNextPoint();
        }
    }

    private static Twins<ArrayList<Point>, Integer> gotoTmpPoint(Robot rob1, Robot rob2) {
        // rob1 给rob2让路，rob1去找临时点
        ArrayList<Point> path = Const.path.getHidePointPath(rob1.pos, rob2.route.getLeftPath());
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
        if (path == null){
            return new Twins<>(null,Const.unreachableFps);
        }
        int extraFps = path.size()-1 - rob1.route.leftPathLen();
        return new Twins<>(path,extraFps);
    }

    private static Twins<Robot,Robot> getCoreRobots(ArrayList<Robot> team) {
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
        if (next.clacGridDis(pos)>=2){
            boolean success = changeRoad(route.target);
            if (success){
                updateNextPoint();
            }else return;   // 找不到路
        }
        frameRobotMove.add(this);
        // 交由后面统一处理移动信息
    }

    private void moveRobotBack() {
        // 机器人后退一格
        next = route.getLastPoint();
    }

    // 打印自己的移动信息
    public void printMove(){
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


    // 切换任务
    private void convertTask() {
        if (arriveGood()){
            // 1、如果到达了物品，捡起物品，换路线选择泊口
            if (bookGood.isExist()){
                Util.printGet(id);
                changeRoad(bookBerth.pos);
            }else {
                // 物品不存在，任务结束
                turnOffTask();
            }
        }else if (arriveBerth()){
            // 2、如果到达了泊口，卸货，任务结束
            Util.printPull(id);
            bookBerth.addGood(bookGood);
            turnOffTask();
        }else {
            Util.printErr("convertTask!");
        }
    }

    private boolean arriveBerth() {
        return bookBerth.pos.equals(pos);
    }

    private boolean arriveGood() {
        return bookGood.pos.equals(pos);
    }

    // 到达了目的地
    private boolean arriveTarget() {
        return route.target.equals(pos);
    }

    // 选择一个任务
    private boolean pickNewTask() {
        boolean picked = pickBerthAndGood();
        if (picked){
            boolean flag = changeRoad(bookGood.pos);
            if (!flag) return false;        // 不能到达该路
            turnOnTask();
            Util.printLog("picked task robot:"+id + ",good"+bookGood+"berth:"+bookBerth);
        }
        return picked;
    }

    @Override
    public String toString() {
        Point next=null;
        Point target=null;
        if (route != null){
            next = route.peekNextPoint();
            target = route.target;
        }
        return "Robot{" +
                "id=" + id +
                "," + pos +
                ", next=" + next +
                ", target=" + target +
                '}';
    }

    // 选择物品和泊口
    private boolean pickBerthAndGood() {
        // 1、先选择与自己最近的泊口
        // 2、选择与泊口最近的物品
        Berth closest = pickBerth();
        Good good = closest.getBestGood();
        if (good != null){
            setBook(good,closest);
            return true;
        }
        return false;
    }

    //预定物品
    private void setBook(Good good, Berth berth) {
        bookGood = good;
        good.setBook(this);
        bookBerth = berth;
        berth.setBook(good);
    }

    // 选择泊口
    private Berth pickBerth() {
        int minFps = 1000000000;
        Berth target=null;
        for (Berth berth: Const.berths){
            int fps = Const.path.getPathFps(pos,berth.pos);
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
    public boolean changeRoad(Point pos){
        route.setNewWay(pos);
        if (route.target != pos){
            Util.printLog("ERROR! changeRoad 找不到路");
            return false;
        }
        return true;
    }
}