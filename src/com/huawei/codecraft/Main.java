/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2024-2024. All rights reserved.
 */

package com.huawei.codecraft;

import java.io.FileNotFoundException;
import java.util.Random;
import java.util.Scanner;
import com.huawei.codecraft.core.*;
import com.huawei.codecraft.way.Mapinfo;

import static com.huawei.codecraft.Util.*;
import static com.huawei.codecraft.Const.*;


/**
 * Main
 *
 * @since 2024-02-05
 */
public class Main {

    public static void main(String[] args) throws FileNotFoundException {
        initLog();
        init();
        Mapinfo.init(map);
        printOk();
        running();
    }

    public static void running(){
        for (int i = 0; i < totalFrame; i++) {
            input();
            printLog("frameId:"+frameId);
            updateBerth();
            handleFrame();
            printOk();
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
//        for (int i = 0; i < 3; i++) {
//            printLog(robots[i]);
//        }
        for (int i = 0; i < 3; i++) {
            printLog(boats[i]);
            printLog(berths[i]);
        }

        for (int i = 0; i < boat_num; i++) {
            boats[i].schedule();
        }
        for (int i = 0; i < robot_num; i++) {
            robots[i].schedule();
        }

    }
    
    public static void init() {
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


        //初始化机器人
        for (int i = 0; i < robot_num; i++) {
            robots[i] = new Robot(i);
        }
        // 初始化船舶
        for (int i = 0; i < boat_num; i++) {
            boats[i] = new Boat(i);
        }
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
            robots[i].x = inStream.nextInt();
            robots[i].y = inStream.nextInt();
            robots[i].status = inStream.nextInt();
        }
        for(int i = 0; i < boat_num; i ++) {
            boats[i].readsts = inStream.nextInt();
            boats[i].berthId = inStream.nextInt();
        }

        inStream.nextLine();
        String okk = inStream.nextLine();

    }

}
