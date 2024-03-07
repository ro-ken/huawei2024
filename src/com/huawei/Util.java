package com.huawei;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Scanner;

// 工具类
public class Util {
    public static final boolean test = true;    // 是否可写入
    public static final Scanner inStream = new Scanner(System.in);
    public static final PrintStream outStream = new PrintStream(new BufferedOutputStream(System.out));
    public static PrintStream log = null;
    public static PrintStream path = null;

    public static void initLog() throws FileNotFoundException {
        if (test){
            log = new PrintStream("./log.txt");
//            System.setOut(log);//把创建的打印输出流赋给系统。即系统下次向 ps输出
        }
    }

    // 打印日志函数
    public static void printLog(Object info){
        if (test){
            log.println(info);
        }
    }

    public static void Ok(){
        outStream.println("OK");
        outStream.flush();
    }

}
