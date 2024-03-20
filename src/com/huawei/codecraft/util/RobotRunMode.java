package com.huawei.codecraft.util;

import com.huawei.codecraft.Const;
import com.huawei.codecraft.Util;
import com.huawei.codecraft.core.Robot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class RobotRunMode {
    private Robot robot;
    private int priority = 0;
    // 机器人优先级：正常行驶0；紧急模式，避让者1；紧急模式优先者2；
    // 优先级低的给高的让位

    private Robot master;   // 每个节点最多一个master
    private HashSet<Robot> slaveSet = new HashSet<>();    // 一个节点可有多个slave；
    private int startFrame;
    public int waitFrame;
    private Point oriTarget;    // 原始目标
    public ArrayList<Point> masterPath = new ArrayList<>();

    public final int constWaitTime = 2;
    public int hideWaitTime;   // 到临时点要等待的时间

    public RobotRunMode(Robot robot) {
        this.robot = robot;
    }

    // 设为临时躲避接口
    public void setHideMode(Robot master) {
        if (priority != 1){ // 第一次变为绕路，要将目标保存下来
            oriTarget = robot.route.target;
        }
        //互相设置优先级
        master.runMode.beMaster(robot);
        beSlave(master);
    }

    public static boolean isDifferent(Robot rob1, Robot rob2) {
        // 判断是否优先级不同
        return rob1.runMode.priority != rob2.runMode.priority;
    }

    public static Robot selectMaster(Robot rob1, Robot rob2) {
        // 找出优先级高的
        if (rob1.runMode.priority > rob2.runMode.priority){
            return rob1;
        }
        return rob2;
    }

    private void beSlave(Robot myMaster) {
        if (priority == 1){
            // 已经是躲避模式了，换主人
            beFree();
        }
        priority = 1;
        master = myMaster;
        masterPath.addAll(myMaster.route.getLeftPath());
        masterPath.addAll(myMaster.runMode.masterPath);
        startFrame = Const.frameId;
        hideWaitTime = myMaster.runMode.hideWaitTime + constWaitTime;   // 避让点越多，等待时间越久
        waitFrame = 0;
    }

    private void beFree() {
        // 先释放master
        if (master != null){
            master.runMode.releaseSlave(robot);  // master释放自己
        }
        waitFrame = 0;
        startFrame = 0;
        master = null;
        hideWaitTime = 0;
        masterPath.clear();
        if (slaveSet.isEmpty()){
            priority = 0;
        }else {
            priority = 2;
        }
    }

    // master 释放 slave
    private void releaseSlave(Robot robot) {
        Util.printLog("masterRelease:"+this+robot);
        slaveSet.remove(robot);
        if (slaveSet.isEmpty()){
            if (master == null){
                priority = 0;
            }
        }
    }

    private void beMaster(Robot mySlave) {
        if (priority == 0){
            // 只有普通模式才会升级
            priority = 2;
        }
        slaveSet.add(mySlave);
    }

    public boolean isHideMode() {
        // 机器人是否处于避让模式
        return priority == 1;
    }

    public boolean tooLong() {
        // 在这个模式待的时间太长
        return Const.frameId - startFrame >= robot.route.way.size() + hideWaitTime * 2;
    }

    public Point beNormal() {
        // 变回正常状态
        Util.printLog("beNormal:"+robot);
        // 变为普通模式
        beFree();
        return oriTarget;   // 返回原始目标点
    }
    private void freeMe(){
        waitFrame = 0;
        startFrame = 0;
        priority = 0;
        master = null;
    }

    public boolean isNotNormal() {
        return priority !=0;
    }
}
