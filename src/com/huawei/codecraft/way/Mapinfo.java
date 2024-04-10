package com.huawei.codecraft.way;

import com.huawei.codecraft.Const;
import com.huawei.codecraft.core.DeliveryPoint;
import com.huawei.codecraft.util.Point;

import java.util.HashMap;

import static com.huawei.codecraft.Const.*;

/**
 * ClassName: Mapinfo
 * Package: com.huawei.codecraft.way
 * Description: 初始化地图信息
 */
public class Mapinfo {
    public static int[][] map = new int[mapWidth][mapWidth];            // 经过处理得数字化map，方便寻路判断
    public static int[][] originalMap = new int[mapWidth][mapWidth];    // 原始得地图map
    public static int[][] seaMap = new int[mapWidth][mapWidth];          // 经过预处理为船行走的 map，经过了预处理
    public static int[][] costMap = new int[mapWidth][mapWidth];          // 经过预处理为船行走的 map，经过了预处理
    public static HashMap<Point, DeliveryPoint> pointToDeliveryPoint = new HashMap<>(); // 根据点找对应的交货点
    // 私有化构造函数防止外部实例化
    private Mapinfo() {
    }

    /**
     * 初始化地图，将原始地图转为更方便处理的int类型
     *
     * @param inputMap 原始地图
     */
    public static void init(char[][] inputMap) {
        for (int i = 0; i < mapWidth; i++) {
            for (int j = 0; j < mapWidth; j++) {
                originalMap[i][j] = inputMap[i][j];
                switch (inputMap[i][j]) {
                    case 'B':
                    case 'c':
                        map[i][j] = Const.MAINBOTH;  // 陆地海洋主干道
                        break;
                    case '>':
                    case 'R':
                        map[i][j] =  Const.MAINROAD; // 陆地主干道
                        break;
                    case '~':
                    case 'S':
                    case 'K':
                        map[i][j] = Const.MAINSEA; // 海洋主干道
                        break;
                    case '.':
                        map[i][j] =  Const.ROAD; // 陆地
                        break;
                    case '*':
                        map[i][j] =  Const.SEA; // 海洋
                        break;
                    case 'C':
                        map[i][j] =  Const.BOTH; // 海陆立体交通地块
                        break;
                    case '#':
                        map[i][j] = Const.OBSTACLE; // 障碍物
                        break;
                    case 'T':
                        map[i][j] = Const.MAINSEA; // 海洋主干道
                        DeliveryPoint deliveryPoint = new DeliveryPoint(new Point(i, j));
                        deliveryPoints.add(deliveryPoint);
                        pointToDeliveryPoint.put(new Point(i, j), deliveryPoint);
                        break;
                    default:
                        map[i][j] = Const.MAINBOTH; // 默认为0
                        break;
                }
            }
        }
    }

    private static boolean isNearMainSea(int x, int y) {
        for (int[] dir : new int[][]{{0, 1}, {0, -1}, {-1, 0}, {1, 0}}) {
            int newX = x + dir[0];
            int newY = y + dir[1];
            if (isValid(newX, newY) && originalMap[newX][newY] == '~') {
                return true;
            }
            else if (isValid(newX, newY) && originalMap[newX][newY] == 'K') {
                if (!isWalkMainBoth) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void initSeaMap() {
        for (int i = 0; i < mapWidth; i++) {
            for (int j = 0; j < mapWidth; j++) {
                if (map[i][j] == Const.MAINROAD || map[i][j] == Const.OBSTACLE || map[i][j] == Const.ROAD) {
                    seaMap[i][j] = Const.ROAD;
                }
                else {
                    seaMap[i][j] = map[i][j];
                }
            }
        }

        for (int i = 0; i < mapWidth; i++) {
            for (int j = 0; j < mapWidth; j++) {
                if (seaMap[i][j] == ROAD) {
                    costMap[i][j] = unreachableFps;
                }
                else  if (originalMap[i][j] == '~') {
                    costMap[i][j] = 2;
                }
                else if (isNearMainSea(i, j)) {
                    costMap[i][j] = 2;
                }
                else if (originalMap[i][j] == 'K') {
                    costMap[i][j] = 1;
                     if (isWalkMainBoth) {
                         costMap[i][j] = 2;
                     }
                }
                else {
                    costMap[i][j] = 1;
                }
            }
        }
    }

    /**
     * 判断地图是否有效
     *
     * @param x 横坐标
     * @param y 纵坐标
     */
    public static boolean isValid(int x, int y) {
        return x >= 0 && y >= 0 && x < mapWidth && y < mapWidth;
    }

}
