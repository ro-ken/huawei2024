package com.huawei.codecraft;
import com.huawei.codecraft.core.Berth;
import com.huawei.codecraft.core.Boat;
import com.huawei.codecraft.core.Robot;
import com.huawei.codecraft.util.BerthArea;
import com.huawei.codecraft.util.Point;
import com.huawei.codecraft.util.RegionValue;
import com.huawei.codecraft.util.Twins;
import com.huawei.codecraft.zone.Region;
import com.huawei.codecraft.zone.RegionManager;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Scanner;

import static com.huawei.codecraft.Const.*;

// 工具类
public class Util {
    public static final boolean test = true;    // 是否可写入
    public static  Scanner inStream = new Scanner(System.in);
    public static final PrintStream outStream = new PrintStream(new BufferedOutputStream(System.out));
    public static PrintStream log = null;

    public static void initLog() throws FileNotFoundException {
        if (test){
            log = new PrintStream("./debug.txt");
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
        Thread.sleep(30);

        int boatValue = 0;
        int boatNum = 0;
        for (Boat boat : boats) {
            boatValue += boat.totalCarryValue;
            boatNum += boat.goodSize;
        }


        printBoth("------结果分析：------");
        printBoth("机器人数："+robot_num +"，花费："+robot_num*2000+"，轮船数："+boat_num+"，花费："+boat_num*8000+"，计算得分:"+(25000+totalSellValue-robot_num*2000-boat_num*8000-boatValue));
        printBoth("------运货信息：------");
        printBoth("总计生成货物："+ countGoodNum+"，总计价值："+countGoodValue+"，单位价值："+countGoodValue/countGoodNum);
        printBoth("搬运码头货物："+ totalCarrySize+"，总计价值："+ totalCarryValue+"，单位价值："+totalCarryValue/(totalCarrySize+0.01));
        printBoth("成功运输货物："+ totalSellSize+"，总计价值："+totalSellValue+"，单位价值："+totalSellValue/(totalSellSize+0.01));
        printBoth("轮船剩余货物：" + boatNum + "剩余价值："+boatValue);
        printLog("----------------泊口平均运货-------------------");
        for (Berth berth : berths) {
            if (berth.myAreas.isEmpty()) continue;
            BerthArea area = berth.myAreas.get(berth.myAreas.size() - 1);
            float expNum = (float) area.getExpGoodNum();
            if (berth.myAreas.size()>1){
                expNum += (float) berth.staticValue.get(berth.myAreas.size()-1).getGoodNum();
            }
            float expMinValue = (float) area.getExpMinValue();
            int expDis = berth.myAreas.get(berth.myAreas.size()-1).getExpMaxStep();
            printLog(berth+"期望运货："+expNum+",\t期望价值："+(float)avgGoodValue*expNum+",\t单位价值："+avgGoodValue+"，\t期望距离"+expDis+",\t期望最低价值："+expMinValue);
            printLog(berth+"实际产生："+area.totalGoodNum/15.0f+",\t实际价值："+area.totalGoodValue/15.0f+",\t单位价值："+area.totalGoodValue*1.0f/area.totalGoodNum+"，\t周期等待:"+area.waitTime/15.0f);
            printLog(berth+"实际运货："+berth.totalCarrySize/15.0f+",\t周期价值：" + berth.totalCarryValue/15.0f+",\t单位价值：" + berth.totalCarryValue*1.0f/berth.totalCarrySize+"，\t取货距离:"+berth.totalDis*1.0f/berth.totalCarrySize);

            printLog("----");
        }
        printBoth("-------------------");
        printBoth("总共跳帧："+dumpFrame);
        printBoth("------机器人详细信息见日志：------");
//        printRobotArea();
        printBerthArea();
//        printLog("平均价值：");
//        printLog(avg);
    }

    public static void printRobotArea() {
        // 机器人泊口信息
        for (Robot robot : robots) {
            printLog(robot);
            for (BerthArea area : robot.areas) {
                printLog(area);
            }
            printLog("----");
        }
    }
    public static void printBerthArea() {
        // 机器人泊口信息
        printLog("打印泊口area信息:");
        for (Berth berth : berths) {
            for (BerthArea myArea : berth.myAreas) {
                printLog(myArea);
            }
            printLog("----");
        }
    }
    public static void printBerthRegion() {
        // 机器人泊口信息
        for (Berth berth : berths) {
            printLog("打印泊口region信息："+berth);
            for (RegionValue value : berth.staticValue.values()) {
                printLog(value);
            }
            printLog("----");
        }
    }

    private static void printBoth(Object info) {
        System.err.println(info);
        printLog(info);
    }

    public static void robotRight(int id){
        printMove(id,RIGHT);
    }
    public static void robotLeft(int id){
        printMove(id,LEFT);
    }
    public static void robotUp(int id){
        printMove(id,UP);
    }
    public static void robotDown(int id){
        printMove(id,DOWN);
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
        printLog("尝试在"+pos+"处购买一个机器人");
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
        printLog("boat："+boatId+"离港");
        outStream.printf("dept %d\n", boatId);
    }
    // 将对应船停靠到泊位上，船进入恢复状态
    public static void boatBerth(int boatId){
        printLog("boat："+boatId+"靠泊");
        outStream.printf("berth %d\n", boatId);
    }
    // 旋转船
    private static void boatRot(int boatId,int d){
        outStream.printf("rot %d %d\n", boatId,d);
    }
    // 顺时针
    public static void boatClockwise(int boatId){
        boatRot(boatId,0);
    }
    // 逆时针
    public static void boatAnticlockwise(int boatId){
        boatRot(boatId,1);
    }
    public static void buyBoat() {
        if (money < 8000){
            return;
        }
        Boat boat = null;
        if (boat_num == 0){
            boat = new Boat(0,boatBuyPos.get(0));
            boatBuy(boatBuyPos.get(0));
        }else {
            boat = Boat.buySecondBoat();
            boatBuy(boat.pos);
        }
        money -= 8000;
        boats.add(boat);
        boat_num++;
    }

    public static void buyRobotArea() {
        // robot前期工作以做完
        while (money >= 2000 && !preAssignRobot.isEmpty()){
            Robot robot = preAssignRobot.remove(0);
            robotBuy(robot.pos);
            money -= 2000;
            robot.pickRegion();
            robots.add(robot);
        }
    }
    public static boolean buyRobot(Point pos) {
        if (money < 2000){
            return false;
        }
        robotBuy(pos);

        money -= 2000;
        Robot robot = new Robot(robots.size(),pos);
        robot.pickRegion();
        robots.add(robot);
        return true;
    }

    public static void processMap() {
        // 后期为节省初始化时间可和mapInfo合并
        for (int i = 0; i < mapWidth; i++) {
            for (int j = 0; j < mapWidth; j++) {
                char ch = map[i][j];
                Point t = new Point(i, j);
                if (ch == 'R') {     // 机器人租赁点
                    robotBuyPos.add(t);
                } else if (ch == 'S') {   // 船舶租赁点
                    boatBuyPos.add(t);
                } else if (ch == 'T') {   // 交货点
                    boatDeliveries.add(t);
                } else if (ch == '.') {
                    totalLandPoint ++;
                }
            }
        }
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

    public static void printBerthAreaTwins(Twins<BerthArea, BerthArea> tp) {
        Util.printDebug("最优化结果:"+tp.getObj1());
        Util.printDebug("最优化结果:"+tp.getObj2());
        Util.printDebug("累计物品数："+(tp.getObj1().getExpGoodNum()+tp.getObj2().getExpGoodNum())+"距离："+tp.getObj1().berth.getPathFps(tp.getObj2().berth.pos));
        Util.printDebug("累计和："+(tp.getObj1().berth.getPathFps(tp.getObj2().berth.pos)*2 + tp.getObj1().getWorkTime() + tp.getObj2().getWorkTime()));
        Util.printDebug("---");
    }

    public static void menuBuy() {
        //                ArrayList<BerthArea> list = new ArrayList<>();
//                Twins<BerthArea,BerthArea> tp;
//                Robot robot = Util.buyRobot(robotBuyPos.get(0));
////
//                if (berths.get(3).myAreas.isEmpty()){
//                    BerthArea area = berths.get(3).getLeftBestArea();
//                    list.add(area);
//                }else {
//                    if (berths.get(0).myAreas.size()<2){
//                        tp = Berth.getTwinsBerthArea(berths.get(0),berths.get(1));
//                        Util.printLog(111);
//                    }else {
//                        tp = Berth.getTwinsBerthArea(berths.get(4),berths.get(5));
//                        Util.printLog(333);
//                    }
//                    list.add(tp.getObj1());
//                    list.add(tp.getObj2());
//                }

//                BerthArea area = null;
//                if (berths.get(0).myAreas.size()<2){
//                    area = berths.get(0).getLeftBestArea();
//                } else if (berths.get(1).myAreas.size() < 2) {
//                    area = berths.get(1).getLeftBestArea();
//                } else if (berths.get(2).myAreas.size() < 2) {
//                    area = berths.get(2).getLeftBestArea();
//                }else if (berths.get(3).myAreas.size() < 2) {
//                    area = berths.get(3).getLeftBestArea();
//                }else if (berths.get(4).myAreas.size() < 2) {
//                    area = berths.get(4).getLeftBestArea();
//                }else if (berths.get(5).myAreas.size() < 2) {
//                    area = berths.get(5).getLeftBestArea();
//                }
//
//                list.add(area);

//                if (robot != null) {
//                    robot.setAreas(list);
//                    Util.printLog(robot+"分配成功"+list);
//                    Util.printBerthArea();
//                }
    }
}
