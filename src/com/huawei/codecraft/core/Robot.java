package com.huawei.codecraft.core;

import com.huawei.codecraft.Const;
import com.huawei.codecraft.Util;
import com.huawei.codecraft.util.Point;

//机器人
public class Robot {
    public int id;     // 机器人编号
    public int x, y;    //   当前位置
    public int carry;   // 是否携带货物，1：携带，0：未携带
    public int status;  // 机器人状态，0：恢复状态，1：正常运行
    public int taskStatus;    // 0：无任务，1：有任务

    public Good bookGood;  // 预定的产品
    public Berth bookBerth;  // 预定的产品
    public Route route;

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

    // 根据路线移动
    private void moveRobot() {
        Point next = route.getNextPoint();
        if (next.clacGridDis(x,y)>=2){
            setRoute(route.target);
            next = route.getNextPoint();
        }
        if (next.x>x){
            Util.printDown(id);
        }else if (next.x<x){
            Util.printUp(id);
        } else if (next.y>y) {
            Util.printRight(id);
        } else if (next.y<y) {
            Util.printLeft(id);
        }
    }

    // 切换任务
    private void convertTask() {
        if (arriveGood()){
            // 1、如果到达了物品，捡起物品，换路线选择泊口
            if (bookGood.isExist()){
                Util.printGet(id);
                setRoute(bookBerth.pos);
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
        return bookBerth.pos.equals(x,y);
    }

    private boolean arriveGood() {
        return bookGood.pos.equals(x,y);
    }

    // 到达了目的地
    private boolean arriveTarget() {
        if (route == null){
            return false;
        }
        return route.target.equals(x, y);
    }

    // 选择一个任务
    private boolean pickNewTask() {
        boolean picked = pickBerthAndGood();
        if (picked){
            turnOnTask();
            setRoute(bookGood.pos);
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
                ", (" + x +
                ", " + y +
                "), next=" + next +
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
        int minFps = 10000;
        Berth target=null;
        for (Berth berth: Const.berths){
            int fps = Const.path.getPathFps(new Point(x,y),berth.pos);
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
    private void setRoute(Point pos) {
        route = new Route(pos,this);
    }
    private void clearRoute() {
        route = null;
    }

    public Point getPos() {
        return new Point(x,y);
    }
}