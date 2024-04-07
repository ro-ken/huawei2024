/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2024-2024. All rights reserved.
 */

package com.huawei.codecraft;

import com.huawei.codecraft.core.Berth;
import com.huawei.codecraft.core.Boat;
import com.huawei.codecraft.core.Good;
import com.huawei.codecraft.core.Robot;
import com.huawei.codecraft.util.BerthArea;
import com.huawei.codecraft.util.Point;
import com.huawei.codecraft.util.Twins;
import com.huawei.codecraft.way.Mapinfo;
import com.huawei.codecraft.zone.RegionManager;

import java.io.FileNotFoundException;
import java.util.ArrayList;

import static com.huawei.codecraft.Const.*;
import static com.huawei.codecraft.Util.*;


/**
 * Main
 *
 * @since 2024-02-05
 */
public class Main {
    public static int testRobot = 100;    // 测试机器人
    public static boolean globalGreedy = true;  // 若本区域没物品，全局贪心，局部贪心
    public static boolean dynamicRegion = false;      // 是否动态分区
    public static boolean areaSched = false;


    public static void main(String[] args) throws FileNotFoundException, InterruptedException {
        initLog();
        readInit();
        myInit();
        printOk();
        running();
        printLastInfo();
    }


    public static void running() {
        while (frameId < totalFrame) {
            input();
            printLog("---------------------frameId:" + frameId + "----------------------");
            long sta = System.nanoTime();
            frameInit();
            long t1 = System.nanoTime();
            handleFrame();
            printOk();
            long end = System.nanoTime();
            printLog("单帧花费时间:" + (end - sta) / 1000 + "us" + ",frameInit时间:" + (t1 - sta) / 1000 + "us,handleFrame时间:" + (end - t1) / 1000 + "us");
        }
    }

    // 追加初始化工作
    private static void myInit() {
        // 初始化顺序很重要，不能乱放
        Mapinfo.init(map);
        Mapinfo.initSeaMap();
        for (Berth berth : berths) {
            pointToBerth.put(berth.pos, berth);
            idToBerth.put(berth.id, berth);
        }
        regionManager = new RegionManager();
        regionManager.init();
        for (Berth berth : berths) {
            berth.init();

        }
        printBerthRegion();
    }


    private static void handleFrame() {

        // 处理轮船调度
        for (Boat boat : boats) {
            boat.schedule();
            boat.updateNextPoint();
            Util.printLog(boat);
        }
        Boat.handleBoatMove();

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
        updateGoodInfo();      // 更新物品信息
        invalidPoints.clear();  //
        workRobots.clear();     // 每帧初始化
        int num = Math.min(testRobot, robot_num);
        for (int i = 0; i < num; i++) {
            workRobots.add(robots.get(i));
            robots.get(i).frameMoved = false;
        }
        for (Boat boat : boats) {
            boat.frameMoved = false;
        }

        if (robot_num < 12) {
            if (areaSched) {

                if (robot_num<5){
                    Robot.buyRobot();
                    return;
                }
                ArrayList<BerthArea> list = new ArrayList<>();
                Twins<BerthArea,BerthArea> tp;
                Robot robot = Util.buyRobot(robotBuyPos.get(0));
//
                if (berths.get(3).myAreas.isEmpty()){
                    BerthArea area = berths.get(3).getLeftBestArea();
                    list.add(area);
                }else {
                    if (berths.get(0).myAreas.size()<2){
                        tp = Berth.getTwinsBerthArea(berths.get(0),berths.get(1));
                        Util.printLog(111);
                    }else {
                        tp = Berth.getTwinsBerthArea(berths.get(4),berths.get(5));
                        Util.printLog(333);
                    }
                    list.add(tp.getObj1());
                    list.add(tp.getObj2());
                }




//                BerthArea area = null;
//                if (berths.get(0).myAreas.size()<2){
//                    area = berths.get(0).getLeftBestArea();
//                } else if (berths.get(1).myAreas.size() < 2) {
//                    area = berths.get(1).getLeftBestArea();
//                } else if (berths.get(2).myAreas.size() < 2) {
//                    area = berths.get(2).getLeftBestArea();
//                }else if (berths.get(3).myAreas.size() < 2) {
//                    area = berths.get(3).getLeftBestArea();
//                }else if (berths.get(4).myAreas.size() < 2) {
//                    area = berths.get(4).getLeftBestArea();
//                }else if (berths.get(5).myAreas.size() < 2) {
//                    area = berths.get(5).getLeftBestArea();
//                }
//
//                list.add(area);



                if (robot != null) {
                    for (BerthArea area1 : list) {
                        area1.enable(robot);
                    }
                    robot.setAreas(list);
                    Util.printLog(robot+"分配成功"+list);
                    Util.printBerthArea();
                }
            } else {
                Point pos = robotBuyPos.get(0);
                buyRobot(pos);
            }
        }
        if (boat_num < 1) {
            buyBoat(boatBuyPos.get(0));
        }

//        if (avg.size() < 1000 && frameId % 10 == 0) {
//            avg.put(frameId, (int) avgGoodValue);
//        }
    }

    private static void updateGoodInfo() {
        if (!frameGoods.isEmpty()) {
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
        for (int i = 0; i < mapWidth; i++) {
            char[] line = inStream.nextLine().toCharArray();
            map[i] = line;
        }
        processMap();
        berth_num = inStream.nextInt();
        // 初始化泊位
        for (int i = 0; i < berth_num; i++) {
            int id = inStream.nextInt();
            Berth berth = new Berth(id);
            berth.core.x = inStream.nextInt();  // 以右下的那个点作为这个泊位的代表点
            berth.core.y = inStream.nextInt();
            berth.loading_speed = inStream.nextInt();
            berth.setPos();
            berths.add(berth);
            printLog("泊口：" + berth);
        }
        Boat.capacity = inStream.nextInt();
        printLog("船的容量：" + Boat.capacity);
        inStream.nextLine();
        String okk = inStream.nextLine();
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
            if (val > 0) {   // 暂时只记录新增货物，不处理消失货物
                Good good = new Good(x, y, val, frameId);
                frameGoods.add(good);
            }
        }
        robot_num = inStream.nextInt();
        if (robot_num != robots.size()) {
            printErr("robot_num != robots.size()");
        }
        for (int i = 0; i < robot_num; i++) {
            int id = inStream.nextInt();    // 机器人id
            robots.get(id).carry = inStream.nextInt();
            robots.get(id).pos.x = inStream.nextInt();
            robots.get(id).pos.y = inStream.nextInt();
        }
        boat_num = inStream.nextInt();
        if (boat_num != boats.size()) {
            printErr("boat_num != boats.size()");
        }
        for (int i = 0; i < boat_num; i++) {
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
