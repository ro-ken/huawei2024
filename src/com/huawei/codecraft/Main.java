/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2024-2024. All rights reserved.
 */

package com.huawei.codecraft;

import java.io.FileNotFoundException;
import java.util.Random;
import java.util.Scanner;
import com.huawei.codecraft.core.*;
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
        printOk();
        running();
    }

    public static void running(){
        for (int i = 0; i < totalFrame; i++) {
            input();
            printLog("frameId:"+frameId);
            handleFrame();
            printOk();
        }
    }

    private static void handleFrame() {
        Random rand = new Random();
        for(int i = 0; i < robot_num; i ++)
            System.out.printf("move %d %d" + System.lineSeparator(), i, rand.nextInt(4) % 4);
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
        for (int i = 1; i <= goodsNum; i++) {
            int x = inStream.nextInt();
            int y = inStream.nextInt();
            int val = inStream.nextInt();
            Good good = new Good(x,y,val,frameId);
            ObjectMap[x][y] = good;
        }
        for(int i = 0; i < robot_num; i++) {

            robots[i].goods = inStream.nextInt();
            robots[i].x = inStream.nextInt();
            robots[i].y = inStream.nextInt();
            robots[i].status = inStream.nextInt();
        }
        for(int i = 0; i < boat_num; i ++) {
            boats[i].status = inStream.nextInt();
            boats[i].berthId = inStream.nextInt();
        }

        inStream.nextLine();
        String okk = inStream.nextLine();
    }

}
