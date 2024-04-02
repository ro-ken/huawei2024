/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2024-2024. All rights reserved.
 */

package com.huawei.codecraft;

import com.huawei.codecraft.core.Berth;
import com.huawei.codecraft.core.Boat;
import com.huawei.codecraft.core.Good;
import com.huawei.codecraft.core.Robot;
import com.huawei.codecraft.util.Point;
import com.huawei.codecraft.way.Mapinfo;
import com.huawei.codecraft.zone.RegionManager;
import java.io.FileNotFoundException;
import static com.huawei.codecraft.Const.*;
import static com.huawei.codecraft.Util.*;


/**
 * Main
 *
 * @since 2024-02-05
 */
public class Main {
    public static int testRobot = 100;    // 测试机器人
    public static int totalSellValue = 0;
    public static int totalSellSize = 0;
    public static int totalCarryValue = 0;
    public static int totalCarrySize = 0;
    public static boolean globalGreedy = true;  // 若本区域没物品，全局贪心，局部贪心
    public static boolean dynamicRegion = false;      // 是否动态分区
    public static int lastFrameId = 0;
    public static int dumpFrame = 0;    // 跳帧记录

    public static void main(String[] args) throws FileNotFoundException, InterruptedException {
        initLog();
        readInit();
        myInit();
        printOk();
        running();
        printLastInfo();
    }


    public static void running(){
        while (frameId < totalFrame) {
            input();
            printLog("-------------frameId:"+frameId+"--------------");
            long sta = System.nanoTime();
            frameInit();
            long t1 = System.nanoTime();
            handleFrame();
            printOk();
            long end = System.nanoTime();
            printLog("单帧花费时间:"+(end-sta)/1000+"us"+"frameInit时间:"+(t1-sta)/1000+"us,handleFrame时间:"+(end-t1)/1000+"us");
        }
    }

    // 追加初始化工作
    private static void myInit() {
        Mapinfo.init(map);

        for (Berth berth:berths) {
            pointToBerth.put(berth.pos,berth);
            idToBerth.put(berth.id, berth);
        }
        regionManager = new RegionManager();
        regionManager.init();

//
//        Util.printDebug("打印区域信息");
//        for (Region region : RegionManager.regions) {
//            Util.printLog(region+":"+region.berths);
//        }
//        testRegionValue();
    }


    private static void handleFrame() {

//        // 处理轮船调度
//        for (Boat boat : boats) {
//            boat.schedule();
//            boat.updateNextPoint();
//        }
//        Boat.handleBoatMove();

        // 处理机器人调度
        for (Robot workRobot : workRobots) {
            workRobot.schedule();   // 调度
            workRobot.updateNextPoint();  // 去下一个点
        }
        // 统一处理移动信息
        Robot.handleRobotMove();
    }

    // 每一帧开始的初始化工作
    private static void frameInit() {
        handleDumpFrame();  // 记录跳帧
        updateGoodInfo();     // todo 重写更新信息
        invalidPoints.clear();  //
        workRobots.clear();     // 每帧初始化
        int num = Math.min(testRobot,robot_num);
        for (int i = 0; i < num; i++) {
            workRobots.add(robots.get(i));
            robots.get(i).frameMoved = false;
        }
        if (frameId == 1){
            // 买机器人，轮船，一艘船，6个机器人
            Point pos = robotBuyPos.get(0);
            buyRobot(pos);
//            buyBoat(boatBuyPos.get(0));
        }else {
            Point pos = robotBuyPos.get(0);
            if (money >= 2000){
                buyRobot(pos);
            }
        }
    }

    private static void updateGoodInfo() {
        if (!frameGoods.isEmpty()){
            for (Good frameGood : frameGoods) {
                printLog(frameGood);
            }
            countGoodNum += frameGoods.size();
            for (Good good : frameGoods) {
                countGoodValue += good.value;
                regionManager.addNewGood(good);
            }
            avgGoodValue = countGoodValue / countGoodNum;
        }
    }

    public static void readInit() {
        // 初始化地图
        for(int i = 0; i < mapWidth; i++) {
            char[] line = inStream.nextLine().toCharArray();
            map[i] = line;
        }
        ProcessMap();
        berth_num = inStream.nextInt();
        // 初始化泊位
        for (int i = 0; i < berth_num; i++) {
            int id = inStream.nextInt();
            Berth berth = new Berth(id);
            berth.pos.x = inStream.nextInt() ;  // 以右下的那个点作为这个泊位的代表点
            berth.pos.y = inStream.nextInt() ;
            berth.loading_speed = inStream.nextInt();
            berths.add(berth);
            printLog("泊口："+berth);
        }
        Boat.capacity = inStream.nextInt();
        printLog("船的容量："+Boat.capacity);
        inStream.nextLine();
        String okk = inStream.nextLine();
    }

    private static void ProcessMap() {
        for (int i = 0; i < mapWidth; i++) {
            for (int j = 0; j < mapWidth; j++) {
                char ch = map[i][j];
                Point t = new Point(i,j);
                if (ch == 'R'){     // 机器人租赁点
                    robotBuyPos.add(t);
                }else if (ch == 'S'){   // 船舶租赁点
                    boatBuyPos.add(t);
                }else if (ch == 'T'){   // 交货点
                    boatDeliveries.add(t);
                }
            }
        }
    }

    public static void input() {
        frameId = inStream.nextInt();
        money = inStream.nextInt();
        int changeGoodNum = inStream.nextInt();
        frameGoods.clear();
        for (int i = 1; i <= changeGoodNum; i++) {
            int x = inStream.nextInt();
            int y = inStream.nextInt();
            int val = inStream.nextInt();
            if (val > 0){   // 暂时只记录新增货物，不处理消失货物
                Good good = new Good(x,y,val,frameId);
                frameGoods.add(good);
            }
        }
        robot_num = inStream.nextInt();
        if (robot_num != robots.size()){
            printErr("robot_num != robots.size()");
        }
        for(int i = 0; i < robot_num; i++) {
            int id = inStream.nextInt();    // 机器人id
            robots.get(id).carry = inStream.nextInt();
            robots.get(id).pos.x = inStream.nextInt();
            robots.get(id).pos.y = inStream.nextInt();
        }
        boat_num = inStream.nextInt();
        if (boat_num != boats.size()){
            printErr("boat_num != boats.size()");
        }
        for(int i = 0; i < boat_num; i ++) {
            int id = inStream.nextInt();    // 轮船id
            boats.get(id).carry = inStream.nextInt();
            boats.get(id).pos.x = inStream.nextInt();
            boats.get(id).pos.y = inStream.nextInt();
            boats.get(id).direction = inStream.nextInt();
            boats.get(id).readsts = inStream.nextInt();
        }

        inStream.nextLine();
        String okk = inStream.nextLine();

    }
}
