package com.huawei.codecraft.core;

import com.huawei.codecraft.Const;
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

    public Robot(int id) {
        this.id = id;
    }


    public void schedule() {
        if (noTask()){
//            pickTask();
//            turnOnTask();
        }else {
            doTask();
        }
    }

    private void doTask() {

    }

    // 选择一个任务
    private void pickTask() {
        // 选择物品和船舶
        // 1、先选择与自己最近的泊口
        // 2、选择与泊口最近的物品
        Berth closest = pickBerth();
        Good good = closest.getBestGood();
        setBook(good,closest);
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
}