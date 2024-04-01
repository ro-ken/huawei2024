package com.huawei.codecraft;
import com.huawei.codecraft.core.Berth;
import com.huawei.codecraft.util.Point;
import com.huawei.codecraft.zone.Region;
import com.huawei.codecraft.zone.RegionManager;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Scanner;

import static com.huawei.codecraft.Const.*;
import static com.huawei.codecraft.Main.*;

// 工具类
public class Util {
    public static final boolean test = true;    // 是否可写入
    public static  Scanner inStream = new Scanner(System.in) ;
    public static final PrintStream outStream = new PrintStream(new BufferedOutputStream(System.out));
    public static PrintStream log = null;

    public static void initLog() throws FileNotFoundException {
        if (test){
            log = new PrintStream("./debug.txt");
//            System.setOut(log);//把创建的打印输出流赋给系统。即系统下次向 ps输出
        }
    }

    // 打印日志
    public static void printLog(Object info){
        if (test){
            log.println("LOG::" + info);
        }
    }
    public static void printDebug(Object info){
        if (test){
            log.println("DEBUG::" + info);
        }
    }

    public static void printErr(Object info){
        if (test){
            log.println("ERROR::" + info);
        }
    }
    public static void printWarn(Object info){
        if (test){
            log.println("WARN::" + info);
        }
    }

    public static void printOk(){
        outStream.println("OK");
        outStream.flush();
    }

    public static void printLastInfo() throws InterruptedException {
        Thread.sleep(1000);
        System.err.println("------运货信息：------");
        System.err.println("总计生成货物："+ countGoodNum+"总计价值："+countGoodValue+"单位价值："+countGoodValue/countGoodNum);
//        System.err.println("搬运码头货物："+ totalCarrySize+"总计价值："+ totalCarryValue+"单位价值："+totalCarryValue/totalCarrySize);
//        System.err.println("成功运输货物："+ totalSellSize+"总计价值："+totalSellValue+"单位价值："+totalSellValue/totalSellSize);
        System.err.println("-------------------");
        System.err.println("总共跳帧："+dumpFrame);
        printLog("总共跳帧："+dumpFrame);
    }

    public static void robotRight(int id){
        printMove(id,0);
    }
    public static void robotLeft(int id){
        printMove(id,1);
    }
    public static void robotUp(int id){
        printMove(id,2);
    }
    public static void robotDown(int id){
        printMove(id,3);
    }
    // 机器人移动
    private static void printMove(int id,int num){
        outStream.printf("move %d %d\n", id,num);
    }
    public static void robotGet(int id){
        outStream.printf("get %d\n", id);
    }
    public static void robotPull(int id){
        outStream.printf("pull %d\n", id);
    }
    public static void robotBuy(Point pos){
        outStream.printf("lbot %d %d\n", pos.x,pos.y);
    }
    public static void boatBuy(Point pos){
        outStream.printf("lboat %d %d\n", pos.x,pos.y);
    }
    // 将船前进移动一格
    public static void boatShip(int boatId){
        outStream.printf("ship %d\n", boatId);
    }
    // 将船重置到主航道，船进入恢复状态
    public static void boatDept(int boatId){
        outStream.printf("dept %d\n", boatId);
    }
    // 将对应船停靠到泊位上，船进入恢复状态
    public static void boatBerth(int boatId){
        outStream.printf("dept %d\n", boatId);
    }
    // 旋转船
    private static void boatRot(int boatId,int d){
        outStream.printf("dept %d %d\n", boatId,d);
    }
    // 顺时针
    public static void boatClockwise(int boatId){
        boatRot(boatId,0);
    }
    // 逆时针
    public static void boatAnticlockwise(int boatId){
        boatRot(boatId,1);
    }


    //
    public static void printMap(){
        for (int i = 0; i < mapWidth; i++) {
            for (int j = 0; j < mapWidth; j++) {
                log.print(map[i][j]);
            }
            printLog("");
        }
    }

    public static void printPath(ArrayList<Point> Path){
        StringBuilder pathStr = new StringBuilder();
        for (int i = 0; i < Path.size(); i++) {
            pathStr.append(Path.get(i));  // 直接使用 Point 对象，自动调用 toString 方法
            if (i < Path.size() - 1) {
                pathStr.append(" -> ");  // 在点之间添加分隔符
            }
        }
        printLog(pathStr);
    }

    private static void moheitu() {
        // 摸黑图
        int line = 31;
        int count1 = 0;
        int line2 = 39;     // 如果没摸出来的话
        int count2 = 0;
        for (int i = 0; i <mapWidth; i++) {
            if (map[line][i] == '.'){
                count1 ++;
            }
            if (map[line2][i] == '.'){
                count2 ++;
            }
        }
        if (count1 % 3 == 0){
//            mapSeq = 1;
//            testRobot = 0;      // todo   摸完以后必须注释掉   ********* ，摸出一个可以先调参，另外两个机器人数设为0,2，继续区分
        }else if (count1 % 3 == 1){
//            mapSeq = 2;
//            testRobot = 2;  // 派2个机器人，防止有个机器人被卡死
        }else if (count1 % 3 == 2){
//            mapSeq = 3;
//            testRobot = 10;     // 全派出去
        }

////  下面是第一次没测出来
//        if (count1 % 3 == 0){
//            mapSeq = 1;
//            testRobot = 0;
//        }else if (count1 % 3 == 2 && count2 % 2 == 0){
//            mapSeq = 2;
//            testRobot = 2;  // 派2个机器人，防止有个机器人被卡死
//        }else if (count1 % 3 == 2 && count2 % 2 == 1){
//            mapSeq = 3;
//            testRobot = 10;     // 全派出去
//        }
        printLog("摸黑图：count1"+count1+"count2" +count2+ "地图seq："+mapSeq);

    }

    // 手动初始化地图
    private static void initMapSeq() {
        int[][][] berthsPos = {
                {{3, 175}, {15, 176}, {33,176}}, // map1
                {{74, 74}, {74, 122}, {82, 60}}, // map2
        };
        if (map[berthsPos[0][0][0]][berthsPos[0][0][1]] == 'B' && map[berthsPos[0][1][0]][berthsPos[0][1][1]] == 'B' && map[berthsPos[0][2][0]][berthsPos[0][2][1]] == 'B') {
            mapSeq = 1;
        }
        else if (map[berthsPos[1][0][0]][berthsPos[1][0][1]] == 'B' && map[berthsPos[1][1][0]][berthsPos[1][1][1]] == 'B' && map[berthsPos[1][2][0]][berthsPos[1][2][1]] == 'B') {
            mapSeq = 2;
        }
        else {
            mapSeq =  defaultMap;
        }
    }

    private static void testRegionValue() {
        for (Region region : RegionManager.regions) {
            printLog("测试Region------"+region+region.berths);
            Util.printLog(region.staticValue.get(1));
            Util.printLog(region.staticValue.get(2));
            Util.printLog(region.staticValue.get(3));
            for (Berth berth : region.berths) {
                Util.printLog(berth+":"+berth.staticValue);
            }
            Util.printLog(" ");
        }

        printLog("berth 静态价值！");
        for (Berth berth : berths) {
            Util.printLog(berth+":size,"+berth.points+berth.staticValue.get(1));
            Util.printLog(berth+":size,"+berth.points+berth.staticValue.get(2));
            Util.printLog(berth+":size,"+berth.points+berth.staticValue.get(3));
            Util.printLog(" ");
        }
    }

    // 记录跳帧情况
    public static void handleDumpFrame() {
        if (frameId - lastFrameId>1){
            Util.printWarn("已跳帧："+(frameId -lastFrameId-1));
            dumpFrame +=frameId -lastFrameId-1;
        }
        lastFrameId = frameId;
    }
}
