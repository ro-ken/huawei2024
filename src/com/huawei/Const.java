package com.huawei;

import com.huawei.core.Berth;
import com.huawei.core.Boat;
import com.huawei.core.Robot;

// 全局变量类
public class Const {

    public static final int n = 200;
    public static final int robot_num = 10;
    public static final int berth_num = 10;
    public static final int N = 210;

    public static int money, boat_capacity, frameId;
    public static String[] ch = new String[N];
    public static int[][] gds = new int[N][N];

    public static Robot[] robot = new Robot[robot_num + 10];
    public static Berth[] berth = new Berth[berth_num + 10];
    public static Boat[] boat = new Boat[10];
}
