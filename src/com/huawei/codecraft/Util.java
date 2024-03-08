package com.huawei.codecraft;
import com.huawei.codecraft.util.Point;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Scanner;

import static com.huawei.codecraft.Const.*;

// 工具类
public class Util {
    public static final boolean test = true;    // 是否可写入
    public static  Scanner inStream = new Scanner(System.in) ;
    public static final PrintStream outStream = new PrintStream(new BufferedOutputStream(System.out));
    public static PrintStream log = null;
    public static PrintStream path = null;

    public static void initLog() throws FileNotFoundException {
        if (test){
            log = new PrintStream("./debug.txt");
//            System.setOut(log);//把创建的打印输出流赋给系统。即系统下次向 ps输出
        }
    }

    // 打印日志函数
    public static void printLog(Object info){
        if (test){
            log.println(info);
        }
    }

    public static void printOk(){
        outStream.println("OK");
        outStream.flush();
    }

    public static void printRight(int id){
        printMove(id,0);
    }
    public static void printLeft(int id){
        printMove(id,1);
    }
    public static void printUp(int id){
        printMove(id,2);
    }
    public static void printDown(int id){
        printMove(id,3);
    }

    public static void printMove(int id,int num){
        outStream.printf("move %d %d\n", id,num);
    }
    public static void printGet(int id){
        outStream.printf("get %d\n", id);
    }
    public static void printPull(int id){
        outStream.printf("pull %d\n", id);
    }
    // 将船移动到泊位
    public static void printShip(int boatId,int berthId){
        outStream.printf("ship %d %d\n", boatId,berthId);
    }
    // 船驶出至虚拟点
    public static void printGo(int boatId){
        outStream.printf("go %d\n", boatId);
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
}
