package com.huawei.codecraft.way;

import static com.huawei.codecraft.Const.mapWidth;
import static com.huawei.codecraft.Util.printLog;

/**
 * ClassName: Mapinfo
 * Package: com.huawei.codecraft.way
 * Description: 初始化地图信息
 */
public class Mapinfo {
    public static int[][] map = new int[mapWidth][mapWidth];

    // 地形类
    public enum Terrain {
        OBSTACLE(-2),
        SEA(-1),
        LAND(0);

        private final int value;

        Terrain(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static Terrain fromValue(int value) {
            for (Terrain terrain : values()) {
                if (terrain.getValue() == value) {
                    return terrain;
                }
            }
            throw new IllegalArgumentException("Unknown value: " + value);
        }
    }

    // 私有化构造函数防止外部实例化
    private Mapinfo() {
    }

    /**
     * 初始化地图，将原始地图转为更方便处理的int类型
     *
     * @param inputMap 原始地图
     */
    public static void init(char[][] inputMap) {
        for (int i = 0; i < inputMap.length; i++) {
            for (int j = 0; j < inputMap[i].length; j++) {
                switch (inputMap[i][j]) {
                    case 'B':
                        map[i][j] = Terrain.LAND.getValue();  // 陆地，暂时不区分陆地部分和海上部分，统一使用0代表空地
                        break;
                    case '*':
                        map[i][j] = Terrain.SEA.getValue(); // 海洋
                        break;
                    case 'A':
                    case '.':
                        map[i][j] =  Terrain.LAND.getValue();  // 用1表示空地和机器人起始位置
                        break;
                    case '#':
                    default:
                        map[i][j] = Terrain.OBSTACLE.getValue(); // 障碍物
                        break;
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
