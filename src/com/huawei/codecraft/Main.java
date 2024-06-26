/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2024-2024. All rights reserved.
 */

package com.huawei.codecraft;

import com.huawei.codecraft.core.Berth;
import com.huawei.codecraft.core.Boat;
import com.huawei.codecraft.core.Good;
import com.huawei.codecraft.core.Robot;
import com.huawei.codecraft.util.Point;
import com.huawei.codecraft.util.Twins;
import com.huawei.codecraft.way.Mapinfo;
import com.huawei.codecraft.zone.RegionManager;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.huawei.codecraft.Const.*;
import static com.huawei.codecraft.Util.*;


/**
 * Main
 *
 * @since 2024-02-05
 */
public class Main {
    public static int testRobot = 100;    // 测试机器人
    public static int assignRobotNum = 16;   // 手动分配机器人数量，小于等于0 则程序自动分配
    public static int assignBoatNum = 0;   // 分配轮船数量，小于等于0为自动分配
    public static double minAddNumPerRobot = 5.0;   // 若为自动分配，每个周期(20s)买一个机器人最少需要搬运多少物品，否则不买
    public static double minValueCoef = 0.2;    // 本泊口最高价值低于最低这个系数乘以期望时，启用贪心算法
    public static double areaMinValueCoef = 0.8;    // 机器人本区域价值队列最低值系数，机器人默认先拿该价值队列，没有货在贪心，
    public static int greedyMaxDis = 50;    // 用贪心算法，最远离本区域多远
    public static int lastGoFps = 0;      // 最后到达交货点的剩余时间，防止货物卖不出去
    public static boolean fixValue = true;     // 获取物品平均价值是否按照预设的来，还是动态计算，白图运行一次可以固定下来
    public static boolean globalGreedy = true;  // 若本区域没物品，全局贪心，局部贪心
    public static boolean dynamicRegion = true;      // 是否动态分区
    public static boolean areaSched = true;
    public static int[][] menuAssign = new int[2][];    // 手动给轮船分配泊口
    public static Map<Twins<Point,Integer>, Map<Point,ArrayList<Point>>> staticPath = new HashMap<>();   // 手动规划的路径
    public static boolean initFindGood = false;     // 机器人是否需要一开始就去找物品
    public static boolean avgAssignBerthToBoat = false;     // 将泊口平均分配给轮船，打开可能会有路径交错，针对两个虚拟点的时候
    public static int finalFpsUseGreedy = 0;
    public static boolean simpleSched = false;   // 轮船简单贪心调度
    public static int robot5000Num = 10;
//    public static int robot2000Num = 2;
    public static int type = 0;


    public static void main(String[] args) throws FileNotFoundException, InterruptedException {
        long sta = System.currentTimeMillis();
        initLog();
        readInit();
        initMapSeq();
        tiaocan();
        myInit();
        printOk();
        long end = System.currentTimeMillis();
        printLog("初始化时间:" + (end - sta) + "ms");
        running();
        printLastInfo();
    }

    private static void tiaocan() {
        if (mapSeq == 1){
            // 陆地面积 20698
            // 当前线上最高 126522
            assignRobotNum = 7;   // 手动分配机器人数量，小于等于0 则程序自动分配                          16
            assignBoatNum = 2;   // 分配轮船数量，小于等于0为自动分配                                      2
            minValueCoef = 0.25;    // 本泊口最高价值低于最低这个系数乘以期望时，启用贪心算法                 0.25
            greedyMaxDis = 55;    // 用贪心算法，最远离本区域多远                                          55
            expGoodNum = 2300;     // 期望总物品数，官方回答：15/100 * 15000 = 2250                       2300
            fixValue = true;     // 获取物品平均价值是否按照预设的来，还是动态计算，白图运行一次可以固定下来
            avgGoodValue = 67;      // 货物的平均价值，每帧更新,设一个初始值，                                67
            areaMinValueCoef = 0.8;    // 机器人本区域价值队列最低值系数，机器人默认先拿该价值队列，没有货在贪心， 0.8
            lastGoFps = 1;      // 最后到达交货点的剩余时间，防止货物卖不出去
            isWalkMainBoth = true;


            Menu.map1();

        } else if (mapSeq == 2) {
            // 多游走 32275
            // 当前线上最高 121520
            assignRobotNum = 8;   // 手动分配机器人数量，小于等于0 则程序自动分配                           15
            assignBoatNum = 2;   // 分配轮船数量，小于等于0为自动分配                                       2
            minValueCoef = 0.3;    // 本泊口最高价值低于最低这个系数乘以期望时，启用贪心算法                   0.2
            greedyMaxDis = 80;    // 用贪心算法，最远离本区域多远                                          80
            expGoodNum = 2600;     // 期望总物品数，官方回答：15/100 * 15000 = 2250                       2600 陆地面积大调大
            fixValue = true;     // 获取物品平均价值是否按照预设的来，还是动态计算，白图运行一次可以固定下来
            avgGoodValue = 67;      // 货物的平均价值，每帧更新,设一个初始值，                               67
            areaMinValueCoef = 0.7;    // 机器人本区域价值队列最低值系数，机器人默认先拿该价值队列，没有货在贪心，0.7
            lastGoFps = 1;      // 最后到达交货点的剩余时间，防止货物卖不出去
            isWalkMainBoth = true;

            Menu.map2();
            menuAssign[0] = new int[]{0,1};   // 给轮船分配的泊口
            menuAssign[1] = new int[]{2};

        } else if (mapSeq == 3) {
            // 多游走 陆地面积 31574
            // 当前线上最高 140600
            assignRobotNum = 17;   // 手动分配机器人数量，小于等于0 则程序自动分配                            17
            assignBoatNum = 2;   // 分配轮船数量，小于等于0为自动分配                                        1
            minValueCoef = 0.25;    // 本泊口最高价值低于最低这个系数乘以期望时，启用贪心算法                   0.25
            greedyMaxDis = 55;    // 用贪心算法，最远离本区域多远                                           55
            expGoodNum = 2470;     // 期望总物品数，官方回答：15/100 * 15000 = 2250                         2470
            fixValue = true;     // 获取物品平均价值是否按照预设的来，还是动态计算，白图运行一次可以固定下来
            avgGoodValue = 67;      // 货物的平均价值，每帧更新,设一个初始值，                                67
            areaMinValueCoef = 0.8;    // 机器人本区域价值队列最低值系数，机器人默认先拿该价值队列，没有货在贪心， 0.8
            lastGoFps = 1;      // 最后到达交货点的剩余时间，防止货物卖不出去
            isWalkMainBoth = true;
        }
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
            for (Point point : berth.landPoints) {
                pointToBerth.put(point, berth);
            }

            idToBerth.put(berth.id, berth);
        }
        regionManager = new RegionManager();
        regionManager.init();
        Berth.init();
        Boat.init();
        Util.preHeatClass();
        printBerthRegion();
        printBerthArea();
    }


    private static void handleFrame() {

        // 处理轮船调度
        for (Boat boat : boats) {
            boat.schedule();       // 调度
            boat.updateNextPoint();     // 确定下一个点
            Util.printLog(boat);
        }
        Boat.handleBoatMove();

        // 处理机器人调度
        for (Robot robot : workRobots) {
            robot.schedule();   // 调度
            robot.updateNextPoint();  // 确定下一个点
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

        if (boat_num < 1) {
            buyBoat();
        }

        if (areaSched) {
            buyRobotArea();
        }else {
            while (robots.size() < assignRobotNum) {
                int index = robots.size()%robotBuyPos.size();
                boolean success = buyRobot(robotBuyPos.get(index));
                if (!success){
                    break;
                }
            }
        }

        if (boat_num < assignBoatNum) {
            buyBoat();
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
            // todo 可以提前固定
            if (!fixValue){
                avgGoodValue = countGoodValue / countGoodNum;
            }
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
            printErr("robot_num ="+robot_num+",size:"+robots.size());
        }
        for (int i = 0; i < robot_num; i++) {
            int id = inStream.nextInt();    // 机器人id
            robots.get(id).carry = inStream.nextInt();
            robots.get(id).pos.x = inStream.nextInt();
            robots.get(id).pos.y = inStream.nextInt();
//            Util.printLog(robots.get(id)+":"+i);
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
