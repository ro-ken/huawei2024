package com.huawei.codecraft;

import com.huawei.codecraft.core.Berth;
import com.huawei.codecraft.core.Boat;
import com.huawei.codecraft.core.Good;
import com.huawei.codecraft.core.Robot;
import com.huawei.codecraft.way.Path;
import com.huawei.codecraft.way.SimplePathImpl;

import java.lang.reflect.Array;
import java.util.ArrayList;

// 全局变量类
public class Const {

    // 地图宽度
    public static final int mapWidth = 200;
    // 10个机器人
    public static final int robot_num = 10;
    // 10个泊位
    public static final int berth_num = 10;
    // 5艘船
    public static final int boat_num = 5;

    // 总共帧数
    public static final int totalFrame= 5 * 60 * 50;
    public static int money, boat_capacity, frameId;
    // 存放地图字符
    public static char[][] map = new char[mapWidth][mapWidth];
    public static Object[][] ObjectMap = new Object[mapWidth][mapWidth];

//    public static int[][] gds = new int[N][N];

    public static Robot[] robots = new Robot[robot_num];
    public static Berth[] berths = new Berth[berth_num];
    public static Boat[] boats = new Boat[boat_num];
    public static ArrayList<Good> frameGoods= new ArrayList<>();    // 每一帧新产生的货物
    public static Path path = new SimplePathImpl(); // 修改Path实现
}
