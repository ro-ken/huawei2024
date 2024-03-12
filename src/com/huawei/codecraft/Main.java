/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2024-2024. All rights reserved.
 */

package com.huawei.codecraft;

import java.io.FileNotFoundException;
import java.util.*;

import com.huawei.codecraft.core.*;
import com.huawei.codecraft.zone.RegionManager;
import com.huawei.codecraft.util.Point;
import com.huawei.codecraft.way.Mapinfo;

import static com.huawei.codecraft.Util.*;
import static com.huawei.codecraft.Const.*;


/**
 * Main
 *
 * @since 2024-02-05
 */
public class Main {

    public static int testRobot = 10;    // 测试机器人

    public static void main(String[] args) throws FileNotFoundException {
        initLog();
        readInit();
        myInit();
        printOk();
        running();
    }

    // 追加初始化工作
    private static void myInit() {
        Mapinfo.init(map);
        initRobot();
        // 初始化船舶
        for (int i = 0; i < boat_num; i++) {
            boats[i] = new Boat(i);
        }
        for (Berth berth : berths) {
            pointToBerth.put(berth.pos,berth);
        }
        regionManager = new RegionManager(path);
        initZone();
    }

    private static void initZone() {

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

    public static void running(){
        input0();   // 第一帧机器人确定机器人序号
        for (int i = 0; i < totalFrame; i++) {
            printLog("frameId:"+frameId);
            updateBerth();
            handleFrame();
            printOk();
            input();
        }
    }

    private static void updateBerth() {
        for (Good frameGood : frameGoods) {
            printLog(frameGood);
        }
        for (Berth berth:berths) {
            berth.updateGoodList(frameGoods);
        }
    }

    private static void handleFrame() {

        frameInit();

        for (int i = 0; i < boat_num; i++) {
            boats[i].schedule();
        }

        for (Robot workRobot : workRobots) {
            workRobot.schedule();   // 调度
            workRobot.gotoNextPoint();  // 去下一个点
        }
        // 统一处理移动信息
        Robot.printRobotMove();
        // 后续不能在调用workRobots，已被清理
    }

    // 每一帧开始的初始化工作
    private static void frameInit() {
        invalidPoints.clear();  //
        workRobots.clear();     // 每帧初始化
        for (int i = 0; i < robot_num; i++) {
            // 找出所有冻结机器人，
            if (robots[i].status == 0){
                invalidPoints.add(robots[i].pos);
            }else {
                workRobots.add(robots[i]);
            }
        }

        for (int i = 0; i < testRobot; i++) {
//            printLog(boats[i]);
//            printLog(berths[i]);
            printLog(robots[i]);
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
            berths[id].pos.x = inStream.nextInt();
            berths[id].pos.y = inStream.nextInt();
            berths[id].transport_time = inStream.nextInt();
            berths[id].loading_speed = inStream.nextInt();
        }
        boat_capacity = inStream.nextInt();
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
