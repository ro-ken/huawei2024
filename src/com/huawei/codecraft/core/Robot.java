package com.huawei.codecraft.core;

import com.huawei.codecraft.Const;
import com.huawei.codecraft.Util;
import com.huawei.codecraft.util.Point;

import java.util.*;

//机器人
public class Robot {
    public int id;     // 机器人编号
    public Point pos = new Point();
    public int carry;   // 是否携带货物，1：携带，0：未携带
    public int status;  // 机器人状态，0：恢复状态，1：正常运行
    public int taskStatus;    // 0：无任务，1：有任务

    public Good bookGood;  // 预定的产品
    public Berth bookBerth;  // 预定的产品
    public Route route;
    public Point next;  // 当前帧需要移动的下一个点
    public static HashSet<Robot> frameRobotMove = new HashSet<>();  // 所有机器人移动信息
    public Robot(int id) {
        this.id = id;
    }

    public void schedule() {
        if (noTask()){
            boolean picked = pickNewTask();
            if (!picked) return;
        }
        doTask();
    }

    private void doTask() {
        if (arriveTarget()){
            convertTask();
        }
        // 到达目的地，可能任务结束，重新分配
        if (noTask()){
            pickNewTask();
        }
        // 判断该怎么走
        if (route!=null){
            moveRobot();
        }else{
            Util.printLog("doTask ERROR!");
        }
    }

    // 统一打印机器人移动信息
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
            handleConflict(new ArrayList<Robot>(frameRobotMove));
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

    }

    // 根据路线移动
    private void moveRobot() {
        next = route.getNextPoint();
        if (next.clacGridDis(pos)>=2){
            boolean success = setRoute(route.target);
            if (success){
                next = route.getNextPoint();
            }else return;   // 找不到路
        }
        frameRobotMove.add(this);
        // 交由后面统一处理移动信息
    }

    // 打印自己的移动信息
    public void printMove(){
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
                boolean flag = setRoute(bookBerth.pos);
                if (!flag) return;
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
            Util.printLog("convertTask ERROR!");
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
        if (route == null){
            return false;
        }
        return route.target.equals(pos);
    }

    // 选择一个任务
    private boolean pickNewTask() {
        boolean picked = pickBerthAndGood();
        if (picked){
            boolean flag = setRoute(bookGood.pos);
            if (!flag) return false;
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
    private boolean setRoute(Point pos) {
        route = new Route(pos,this);
        if (route.way == null){
            route = null;   //找不到路
            Util.printLog("ERROR! setRoute 找不到路");
            return false;
        }
        return true;
    }
    private void clearRoute() {
        route = null;
    }

}