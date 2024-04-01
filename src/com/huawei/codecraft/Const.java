package com.huawei.codecraft;

import com.huawei.codecraft.core.Berth;
import com.huawei.codecraft.core.Boat;
import com.huawei.codecraft.core.Good;
import com.huawei.codecraft.core.Robot;
import com.huawei.codecraft.util.Point;
import com.huawei.codecraft.way.Path;
import com.huawei.codecraft.way.PathImpl;
import com.huawei.codecraft.zone.RegionManager;
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
    public static int robot_num;
    // 10个泊位
    public static int berth_num;
    // 5艘船
    public static int boat_num;

    // 总共帧数 5分钟
    public static final int totalFrame= 5 * 60 * 50;
    public static final int b2bFps = 500;   // 泊位到泊位时间
    public static int money = 25000;    // 起始资金25000
    public static int frameId;
    // 存放地图字符
    public static char[][] map = new char[mapWidth][mapWidth];
    public static Robot[] robots = new Robot[50];
    public static Berth[] berths = new Berth[50];
    public static Boat[] boats = new Boat[boat_num];
    public static ArrayList<Point> robotBuyPos = new ArrayList<>();      // 机器人购买点
    public static ArrayList<Point> boatBuyPos = new ArrayList<>();       // 轮船购买点
    public static ArrayList<Point> boatDeliveries = new ArrayList<>();       // 轮船交货点
    public static HashSet<Point> mainRoad = new HashSet<>();       // 陆地主干道
    public static HashSet<Point> mainChannel = new HashSet<>();       // 海洋主航道
    public static ArrayList<Good> frameGoods= new ArrayList<>();    // 每一帧新产生的货物
    public static final int noLimitedSize = 1000000;        // 无限容量信息
    public static double expGoodNum = 2450;     // 期望总物品数，官方回答：15/100 * 15000
    public static HashSet<Robot> workRobots = new HashSet<>();// 每帧可以工作的机器人
    public static HashSet<Point> invalidPoints = new HashSet<>();   // 每帧的无效点
    public static int countGoodNum = 0;     //已经生成的物品总数，到时候算平均价值
    public static double countGoodValue = 0;   //已经生成的物品总价值
    public static double avgGoodValue;      // 货物的平均价值，每帧更新

    /*****************************地图相关参数*****************************/
    public static RegionManager regionManager;
    public static Path path = new PathImpl(); // 修改Path实现
    public static int NoLimit = -1;              // 无长度限制的寻路算法
    public static final int unreachableFps = 1000000;       // 不可达的时间
    public static ArrayList<Zone> zones = new ArrayList<>();    // 陆地划分的区域
    public static Map<Point,Berth> pointToBerth = new HashMap<>();  // 左上角位置到泊位的映射
    public static Map<Integer, Berth> idToBerth = new HashMap<>();  // id到泊位的映射
    public static final int defaultMap = 0;
    public static  int mapSeq;
    public static double upperQuantile = 0.06;         // 上分位点，每增加0.02，期望聚合泊位数增加2
    public static int maxThreshold = 40;              // 设定的最大阈值, 超过这个就不合并
    public static double minPointsPercent = 0.045;    // 设定的最小点数百分比，区域拥有泊位少于0.045，则直接不合并
}
