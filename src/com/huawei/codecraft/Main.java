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
import com.huawei.codecraft.zone.Region;
import com.huawei.codecraft.zone.RegionManager;

import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

import static com.huawei.codecraft.Const.*;
import static com.huawei.codecraft.Util.*;


/**
 * Main
 *
 * @since 2024-02-05
 */
public class Main {

    public static int testRobot = 10;    // 测试机器人
    public static int totalValue = 0;
    public static int totalGoodNum = 0;
    public static boolean globalGreedy = true;  // 若本区域没物品，全局贪心，局部贪心
    public static boolean dynamicRegion = false;      // 是否动态分区
    public static boolean boatAvgAssign = true;     // 按照平均时间分配泊口给轮船,否则按照距离分

    public static void main(String[] args) throws FileNotFoundException {
        initLog();
        readInit();
        myInit();
        printOk();
        running();
    }


    public static void running(){
        input0();   // 第一帧机器人确定机器人序号
        for (int i = 0; i < totalFrame; i++) {
            printLog("-------------frameId:"+frameId+"--------------");
//            long sta = System.nanoTime();
            frameInit();
            handleFrame();
            printOk();
//            long end = System.nanoTime();
//            Util.printLog("单帧花费时间:"+(end-sta)/1000+"us");
            input();
        }
    }


    // 追加初始化工作
    private static void myInit() {
        Mapinfo.init(map);
        initRobot();
        for (Berth berth : berths) {
            pointToBerth.put(berth.pos,berth);
            idToBerth.put(berth.id, berth);
        }
        regionManager = new RegionManager();
        regionManager.init();

        initBoat();

        Util.printDebug("打印区域信息");
        for (Region region : RegionManager.regions) {
            Util.printLog(region+":"+region.berths);
        }
//        testRegionValue();
    }

    private static void initBoat() {
        // 初始化船舶
        for (int i = 0; i < boat_num; i++) {
            boats[i] = new Boat(i);
        }
        Boat.init();
    }

    // 初始化机器人信息
    private static void initRobot() {
        int id = 0;
        for (int i = 0; i < mapWidth; i++) {
            for (int j = 0; j < mapWidth; j++) {
                if (map[i][j] == 'A'){
                    // 纵向为x，横向为y
                    robots[id] = new Robot(id++,i,j);
                }
            }
            if (id == robot_num) break; //
        }
    }

    private static void handleFrame() {

        // 处理轮船调度
        for (int i = 0; i < boat_num; i++) {
            boats[i].schedule();
        }

        // 处理机器人调度
        for (Robot workRobot : workRobots) {
            workRobot.schedule();   // 调度
            workRobot.gotoNextPoint();  // 去下一个点
        }
        // 统一处理移动信息
        Robot.printRobotMove();
    }

    // 每一帧开始的初始化工作
    private static void frameInit() {

        updateGoodInfo();
        invalidPoints.clear();  //
        workRobots.clear();     // 每帧初始化
        for (int i = 0; i < testRobot; i++) {
            // 找出所有冻结机器人，
            if (robots[i].status == 0){
                invalidPoints.add(robots[i].pos);
                Util.printDebug("机器人被冻结"+robots[i]);
            }else {
                if (robots[i].region != null){
                    workRobots.add(robots[i]);
                }
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
//        printMap();

        // 初始化泊位
        for (int i = 0; i < berth_num; i++) {
            int id = inStream.nextInt();
            berths[id] = new Berth(id);
            berths[id].pos.x = inStream.nextInt() + 1;  // 以右下的那个点作为这个泊位的代表点
            berths[id].pos.y = inStream.nextInt() + 1;
            berths[id].transport_time = inStream.nextInt();
            berths[id].loading_speed = inStream.nextInt();
            Util.printLog("泊口："+berths[id]);
        }
        Boat.capacity = inStream.nextInt();
        Util.printLog("船的容量："+Boat.capacity);
        inStream.nextLine();
        String okk = inStream.nextLine();
    }

    public static void input() {
        frameId = inStream.nextInt();
        money = inStream.nextInt();
        int goodsNum = inStream.nextInt();
        frameGoods.clear();
        for (int i = 1; i <= goodsNum; i++) {
            int x = inStream.nextInt();
            int y = inStream.nextInt();
            int val = inStream.nextInt();
            Good good = new Good(x,y,val,frameId);
            ObjectMap[x][y] = good;
            frameGoods.add(good);
        }
        for(int i = 0; i < robot_num; i++) {
            robots[i].carry = inStream.nextInt();
            robots[i].pos.x = inStream.nextInt();
            robots[i].pos.y = inStream.nextInt();
            robots[i].status = inStream.nextInt();
        }
        for(int i = 0; i < boat_num; i ++) {
            boats[i].readsts = inStream.nextInt();
            boats[i].berthId = inStream.nextInt();
        }

        inStream.nextLine();
        String okk = inStream.nextLine();

    }
    public static void input0() {
        frameId = inStream.nextInt();
        money = inStream.nextInt();
        int goodsNum = inStream.nextInt();
        frameGoods.clear();
        for (int i = 1; i <= goodsNum; i++) {
            int x = inStream.nextInt();
            int y = inStream.nextInt();
            int val = inStream.nextInt();
            Good good = new Good(x,y,val,frameId);
            ObjectMap[x][y] = good;
            frameGoods.add(good);
        }
        // 以下变化
        Set<Robot> set = new HashSet<>();
        for (int i = 0; i < robot_num; i++) {
            set.add(robots[i]);
        }
        for(int i = 0; i < robot_num; i++) {
            int carry = inStream.nextInt();
            int x = inStream.nextInt();
            int y = inStream.nextInt();
            int status = inStream.nextInt();
            if (!robots[i].pos.equals(x,y)){ // 防止出题组乱序，重新编号
                Robot robot = getRobotByPos(set,new Point(x,y));
                robot.id = i;   // 重新编号
                robots[i] = robot;
            }
            robots[i].carry = carry;
            robots[i].status = status;
        }
        // 以上变化
        for(int i = 0; i < boat_num; i ++) {
            boats[i].readsts = inStream.nextInt();
            boats[i].berthId = inStream.nextInt();
        }

        inStream.nextLine();
        String okk = inStream.nextLine();

    }

    private static Robot getRobotByPos(Set<Robot> set, Point point) {
        for (Robot robot : set) {
            if (robot.pos.equals(point)){
                // 两个位置重合就是
                return robot;
            }
        }
        return null;
    }
}
