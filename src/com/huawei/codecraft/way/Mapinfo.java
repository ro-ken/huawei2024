package com.huawei.codecraft.way;
import com.huawei.codecraft.Const;

public class Mapinfo {
    public static int[][] map = new int[Const.mapWidth][Const.mapWidth];

    // 私有化构造函数防止外部实例化
    private Mapinfo() {
    }

    // 初始化地图，传入字符数组，并将地图转换为整数表示
    public static void init(char[][] inputMap) {
        for (int i = 0; i < inputMap.length; i++) {
            for (int j = 0; j < inputMap[i].length; j++) {
                switch (inputMap[i][j]) {
                    case 'B':
                        map[i][j] = 0;  // 0代表泊位，船只和机器人都可以走,复赛需要额外区分陆地部分和海洋部分
                        break;
                    case '*':
                        map[i][j] = -1; // -1 代表大海
                        break;
                    case '#':
                        map[i][j] = -2; // -2代表障碍物
                        break;
                    case 'A':
                    case '.':
                        map[i][j] = 1;  // 用1表示空地和机器人起始位置
                        break;
                    default:
                        map[i][j] = 1;  // 对未知字符默认使用1
                        break;
                }
            }
        }
    }

    public static boolean isValid(int x, int y) {
        return x >= 0 && y >= 0 && x < map.length && y < map[0].length;
    }
}
