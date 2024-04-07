package com.huawei.codecraft.util;

import com.huawei.codecraft.Const;
import com.huawei.codecraft.core.Good;

// 评判region的价值 该分多少机器人的类
public class RegionValue {
    int num;  // 有多少机器人工作
    boolean areaRich;   // 面积是否充足
    private int expStep;   // 如果机器人正常工作，应该走的期望最远距离，越少越好
    private double expGoodNum;  // 总共经过有多少物品
    private int expMaxStepNum;   // 该区域最远的点有多少个，供下一区域参考，如果已经是最后区域，isrich == false，该值无效
    public static double totalFps = Good.maxSurvive;    // 总共的要走完的FPS

    public boolean isAreaRich() {
        return areaRich;
    }

    public int getExpStep() {
        return expStep;
    }

    public double getGoodNum() {
        return expGoodNum;
    }


    public int getExpMaxStepNum() {
        return expMaxStepNum;
    }

    @Override
    public String toString() {
        return "RegionValue{" +
                "num=" + num +
                ", rich=" + areaRich +
                ", expStep=" + expStep +
                ", expGoodNum=" + expGoodNum +
                '}';
    }

    public RegionValue(int num, boolean areaRich, int step, double expGoodNum,int expMaxStepNum) {
        this.num = num;
        this.areaRich = areaRich;
        this.expStep = step;
        this.expGoodNum = expGoodNum;
        this.expMaxStepNum = expMaxStepNum;
    }

    public double getPeriodValue() {
        double avg = 100;
        // 获取周期内的价值
        if (Const.avgGoodValue != 0){
            avg = Const.avgGoodValue;
        }
        return expGoodNum * avg;
    }

    public double getFpsValue() {
        // 获取单帧价值
        return getPeriodValue()/totalFps;
    }
    public double getSingleRobotFpsValue() {
        // 获取单帧价值
        return getFpsValue()/num;
    }
}
