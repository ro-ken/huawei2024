package com.huawei.codecraft;

import com.huawei.codecraft.core.*;
import com.huawei.codecraft.util.Point;
import com.huawei.codecraft.util.Twins;
import com.huawei.codecraft.way.Path;
import com.huawei.codecraft.way.PathImpl;
import com.huawei.codecraft.zone.RegionManager;
import com.huawei.codecraft.zone.Zone;

import java.util.*;

// 全局变量类
public class Const {
    public static final int mapWidth = 200;  // 地图宽度
    public static int robot_num;
    public static int berth_num;
    public static int boat_num;
    public static final int totalFrame= 5 * 60 * 50;    // 总共帧数 5分钟
    public static int money = 17000;    // 起始资金25000
    public static int frameId;
    // 存放地图字符
    public static char[][] map = new char[mapWidth][mapWidth];
    public static ArrayList<Robot> robots = new ArrayList<>();
    public static ArrayList<Robot> preAssignRobot = new ArrayList<>();  // 预分配机器人序列
    public static ArrayList<Berth> berths = new ArrayList<>();
    public static ArrayList<Boat> boats = new ArrayList<>();
    public static ArrayList<DeliveryPoint> deliveryPoints = new ArrayList<>();      // 交货点
    public static ArrayList<Point> robotBuyPos = new ArrayList<>();      // 机器人购买点
    public static ArrayList<Point> boatBuyPos = new ArrayList<>();       // 轮船购买点
    public static ArrayList<Point> boatDeliveries = new ArrayList<>();       // 轮船交货点
    public static HashSet<Robot> workRobots = new HashSet<>();// 每帧可以工作的机器人
    public static HashSet<Point> invalidPoints = new HashSet<>();   // 每帧的无效点

    /*****************************物品相关参数*****************************/
    public static ArrayList<Good> frameGoods= new ArrayList<>();    // 每一帧新产生的货物
    public static final int noLimitedSize = 1000000;        // 无限容量信息
    public static double expGoodNum = 2470;     // 期望总物品数，官方回答：15/100 * 15000 = 2250
    public static int totalLandPoint;   // 统计陆地点数，用于算每点概率
    public static int countGoodNum = 0;     //已经生成的物品总数，到时候算平均价值
    public static double countGoodValue = 0;   //已经生成的物品总价值
    public static double avgGoodValue = 67;      // 货物的平均价值，每帧更新,设一个初始值，
    /*****************************机器人和轮船对应的方向*****************************/
    public static final int RIGHT = 0;
    public static final int LEFT = 1;
    public static final int UP = 2;
    public static final int DOWN = 3;
    /*****************************地图基础参数定义*****************************/
    public static final int MAINBOTH = 0;        // 海洋、陆地主干
    public static final int MAINROAD = 1;       // 陆地主干道
    public static final int MAINSEA = -1;       // 海洋主干
    public static final int ROAD = 2;           // 陆地
    public static final int SEA = -2;           // 海洋
    public static final int BOTH = 3;           // 陆地、海洋
    public static final int OBSTACLE = -3;      // 障碍
    public static boolean isWalkMainBoth = true; // 是否走主航道
    /*****************************路径相关参数*****************************/
    public static RegionManager regionManager;
    public static Path path = new PathImpl(); // 修改Path实现
    public static int NoLimit = -1;              // 无长度限制的寻路算法
    public static final int unreachableFps = 1000000;       // 不可达的时间
    // 陆地热路径点，key为初始点，value为<目的地，路径>
    public static Map<Point, Map<Point, List<Point>>> landHotPath = new HashMap<>();
    // 海洋热路径点，key为<初始点，方向>，value为<目的地，<路径，路径长度>>
    public static Map<Twins<Point,Integer>,Map<Point,Twins<ArrayList<Point>,Integer>>> seaHotPath = new HashMap<>();
    /*****************************区域相关参数*****************************/
    public static final int defaultMap = 0;
    public static  int mapSeq;
    public static Map<Point,Berth> pointToBerth = new HashMap<>();  // 左上角位置到泊位的映射
    public static Map<Integer, Berth> idToBerth = new HashMap<>();  // id到泊位的映射
    public static ArrayList<Zone> zones = new ArrayList<>();    // 陆地划分的区域
    public static double upperQuantile = 0.06;         // 上分位点，每增加0.02，期望聚合泊位数增加2
    public static int maxThreshold = 40;              // 设定的最大阈值, 超过这个就不合并
    public static double minPointsPercent = 0.045;    // 设定的最小点数百分比，区域拥有泊位少于0.045，则直接不合并
    /*****************************打印相关参数*****************************/
    public static int totalSellValue = 0;
    public static int totalSellSize = 0;
    public static int totalCarryValue = 0;
    public static int totalCarrySize = 0;
    public static int lastFrameId = 0;
    public static int dumpFrame = 0;    // 跳帧记录
    static Map<Integer,Integer> avg = new TreeMap<>();

}
