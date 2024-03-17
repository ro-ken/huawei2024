package com.huawei.codecraft;

import com.huawei.codecraft.core.Berth;
import com.huawei.codecraft.core.Boat;
import com.huawei.codecraft.core.Good;
import com.huawei.codecraft.core.Robot;
import com.huawei.codecraft.zone.RegionManager;
import com.huawei.codecraft.util.Point;
import com.huawei.codecraft.way.Path;
import com.huawei.codecraft.way.PathImpl;
import com.huawei.codecraft.zone.Zone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

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

    // 总共帧数 5分钟
    public static final int totalFrame= 5 * 60 * 50;
    public static final int b2bFps = 500;   // 泊位到泊位时间
    public static int money, frameId;
    // 存放地图字符
    public static char[][] map = new char[mapWidth][mapWidth];
    public static Object[][] ObjectMap = new Object[mapWidth][mapWidth];

    public static Robot[] robots = new Robot[robot_num];
    public static Berth[] berths = new Berth[berth_num];
    public static Boat[] boats = new Boat[boat_num];
    public static ArrayList<Good> frameGoods= new ArrayList<>();    // 每一帧新产生的货物
    public static Path path = new PathImpl(); // 修改Path实现
    public static Map<Point,Berth> pointToBerth = new HashMap<>();  // 左上角位置到泊位的映射
    public static final int unreachableFps = 1000000;       // 不可达的时间
    public static final double upperQuantile = 0.1;         // 上分位点
    public static final int maxThreshold = 40;              // 设定的最大阈值, 超过这个就不合并
    public static double expGoodNum = 2450;     // 期望总物品数，官方回答：15/100 * 15000
    public static HashSet<Robot> workRobots = new HashSet<>();// 每帧可以工作的机器人
    public static HashSet<Point> invalidPoints = new HashSet<>();   // 每帧的无效点
    public static ArrayList<Zone> zones = new ArrayList<>();    // 陆地划分的区域
    public static RegionManager regionManager;
    public static int countGoodNum = 0;     //已经生成的物品总数，到时候算平均价值
    public static double countGoodValue = 0;   //已经生成的物品总价值

}
