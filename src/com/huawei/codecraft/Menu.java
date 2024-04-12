package com.huawei.codecraft;

import com.huawei.codecraft.util.Point;
import com.huawei.codecraft.util.Twins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.huawei.codecraft.Const.*;

public class Menu {

    /**
     * 复赛图1
     */
    public static void map1() {
        setSeaPath(new Point(36,98),3,new Point(50, 170),"[(36,98), (37,98), (38,98), (39,98), (40,98), (41,98), (42,98), (43,98), (44,98), (45,98), (46,98), (47,98), (48,98), (49,97), (49,98), (49,99), (49,100), (49,101), (49,102), (49,103), (49,104), (49,105), (49,106), (49,107), (49,108), (49,109), (49,110), (49,111), (49,112), (49,113), (49,114), (49,115), (49,116), (49,117), (49,118), (49,119), (49,120), (49,121), (49,122), (49,123), (49,124), (49,125), (49,126), (49,127), (49,128), (49,129), (49,130), (49,131), (49,132), (49,133), (49,134), (49,135), (49,136), (49,137), (49,138), (49,139), (49,140), (49,141), (49,142), (49,143), (49,144), (49,145), (49,146), (49,147), (49,148), (49,149), (49,150), (49,151), (49,152), (49,153), (49,154), (49,155), (49,156), (49,157), (49,158), (49,159), (49,160), (49,161), (49,162), (49,163), (49,164), (49,165), (49,166), (49,167), (49,168), (49,169), (49,170), (49,170)]");
    }


    private static void setSeaPath(Point core, int direction, Point target,String strpath) {
        ArrayList<Point> path = parsePoints(strpath);
        Twins<Point, Integer> key = new Twins<>(core, direction);
        Map<Point,ArrayList<Point>> map;
        if (Main.staticPath.containsKey(key)){
            map = Main.staticPath.get(key);
        }else {
            map = new HashMap<>();
            Main.staticPath.put(key,map);
        }
        map.put(target,path);
    }

    public static ArrayList<Point> parsePoints(String input) {
        ArrayList<Point> points = new ArrayList<>();

        // 匹配坐标对的正则表达式
        Pattern pattern = Pattern.compile("\\((\\d+),(\\d+)\\)");
        Matcher matcher = pattern.matcher(input);

        // 循环匹配并提取坐标对
        while (matcher.find()) {
            int x = Integer.parseInt(matcher.group(1));
            int y = Integer.parseInt(matcher.group(2));
            points.add(new Point(x, y));
        }

        return points;
    }

    public static void main(String[] args) {
        map1();
    }

    /**
     * 复赛图2
     */
    public static void map2() {

    }
}