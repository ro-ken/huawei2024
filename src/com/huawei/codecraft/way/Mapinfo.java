package com.huawei.codecraft.way;

import com.huawei.codecraft.Const;
import com.huawei.codecraft.core.DeliveryPoint;
import com.huawei.codecraft.util.Point;

import java.util.ArrayList;
import java.util.HashMap;

import static com.huawei.codecraft.Const.deliveryPoints;
import static com.huawei.codecraft.Const.mapWidth;

/**
 * ClassName: Mapinfo
 * Package: com.huawei.codecraft.way
 * Description: 初始化地图信息
 */
public class Mapinfo {
    public static int[][] map = new int[mapWidth][mapWidth];            // 经过处理得数字化map，方便寻路判断
    public static int[][] originalMap = new int[mapWidth][mapWidth];    // 原始得地图map
    public static int[][] seaMap = new int[mapWidth][mapWidth];          // 经过预处理为船行走的 map，经过了预处理
    public static HashMap<Point, DeliveryPoint> pointToDeliveryPoint = new HashMap<>(); // 根据点找对应的交货点
    public static ArrayList<Point> specialPoint = new ArrayList<>();
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
        // 对于seaMap在做一个特殊处理，将立交桥左右包起来，保证A*找到的点是一个有效点
        // TODO，暂时不做边界测试，因为海路交通肯定是连接海陆的，不应该在边界放
        for (int i = 0; i < mapWidth; i++) {
            for (int j = 0; j < mapWidth; j++) {
                if (originalMap[i][j] == 'c') {
                    if (originalMap[i - 1][j] == '.' && originalMap[i + 1][j] == 'c') {
                        specialPoint.add(new Point(i, j));
                    } else if (originalMap[i - 1][j] == 'c' && originalMap[i + 1][j] == '.') {
                        specialPoint.add(new Point(i, j));
                    } else if (originalMap[i][j - 1] == 'c' && originalMap[i][j + 1] == '.') {
                        specialPoint.add(new Point(i, j));
                    } else if (originalMap[i][j - 1] == '.' && originalMap[i][j + 1] == 'c') {
                        specialPoint.add(new Point(i, j));
                    }
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
