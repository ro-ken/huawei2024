package com.huawei.codecraft.way;

import com.huawei.codecraft.Const;

import static com.huawei.codecraft.Const.ROAD;
import static com.huawei.codecraft.Const.mapWidth;

/**
 * ClassName: Mapinfo
 * Package: com.huawei.codecraft.way
 * Description: 初始化地图信息
 */
public class Mapinfo {
    public static int[][] map = new int[mapWidth][mapWidth];
    public static int[][] seaMap = new int[mapWidth][mapWidth]; // 经过预处理为船行走的 map，经过了预处理

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
                    case 'T':
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
                if (map[i][j] == Const.MAINROAD || map[i][j] == Const.OBSTACLE || map[i][j] == ROAD) {
                    seaMap[i][j] = ROAD;
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
