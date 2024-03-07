/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2024-2024. All rights reserved.
 */

package com.huawei;

import java.io.FileNotFoundException;
import java.util.Random;
import java.util.Scanner;
import com.huawei.core.*;
import static com.huawei.Util.*;
import static com.huawei.Const.*;


/**
 * Main
 *
 * @since 2024-02-05
 */
public class Main {

    public static void main(String[] args) throws FileNotFoundException {
        initLog();
        init();
        running();
    }

    public static void running(){
        for (int i = 0; i < 15000; i++) {
            input();
            printLog(frameId);
            handleFrame();
            Ok();
        }
    }

    private static void handleFrame() {
        Random rand = new Random();
        for(int i = 0; i < robot_num; i ++)
            System.out.printf("move %d %d" + System.lineSeparator(), i, rand.nextInt(4) % 4);
    }
    
    public static void init() {
        Scanner scanf = new Scanner(System.in);
        for(int i = 1; i <= n; i++) {
            ch[i] = scanf.nextLine();
        }
        for (int i = 0; i < berth_num; i++) {
            int id = scanf.nextInt();
            berth[id] = new Berth();
            berth[id].x = scanf.nextInt();
            berth[id].y = scanf.nextInt();
            berth[id].transport_time = scanf.nextInt();
            berth[id].loading_speed = scanf.nextInt();
        }
        boat_capacity = scanf.nextInt();
        String okk = scanf.nextLine();
        Ok();
    }

    public static void input() {
        Scanner scanf = new Scanner(System.in);
        frameId = scanf.nextInt();
        money = scanf.nextInt();
        int num = scanf.nextInt();
        for (int i = 1; i <= num; i++) {
            int x = scanf.nextInt();
            int y = scanf.nextInt();
            int val = scanf.nextInt();
        }
        for(int i = 0; i < robot_num; i++) {
            robot[i] = new Robot();
            robot[i].goods = scanf.nextInt();
            robot[i].x = scanf.nextInt();
            robot[i].y = scanf.nextInt();
            int sts = scanf.nextInt();
        }
        for(int i = 0; i < 5; i ++) {
            boat[i] = new Boat();
            boat[i].status = scanf.nextInt();
            boat[i].pos = scanf.nextInt();
        }
        String okk = scanf.nextLine();
    }

}
