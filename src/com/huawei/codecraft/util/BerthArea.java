package com.huawei.codecraft.util;

import com.huawei.codecraft.Const;
import com.huawei.codecraft.Util;
import com.huawei.codecraft.core.Berth;
import com.huawei.codecraft.core.Good;
import com.huawei.codecraft.core.Robot;

import java.util.Comparator;
import java.util.PriorityQueue;

public class BerthArea {
    // 对单个泊口按照价值进行分区
    public Berth berth;    // 分配的泊口
    public Robot robot;    // 分配的机器人
    public boolean single = true;   //  对应机器人是否只负责当前一个区域，如果是，那么与berth的RegionValue是对应的，如果是false，那么后续无区域
    public PriorityQueue<Good> areaGoodsByTime = new PriorityQueue<>(new Comparator<Good>() {
        @Override
        public int compare(Good o1, Good o2) {
            return o1.deadFrame - o2.deadFrame;
        }
    });    // 泊口价值大于本区域期望价值的物品，按照剩余时间排序
    private int level;  // 泊口的区域级别，通过这个找到自己的队列，从1开始,和RegionValue的robot对应
    private int workFps = Good.maxSurvive;   // 在该区域的工作时间，如果机器人只在此泊口工作，那么为 1000FPS
    private double expGoodNum;     // 机器人在此区域预期产值
    private int expMaxStep;     // 期望最远距离
    private int expMaxStepNum;      // 期望最远距离所占的点数
    private boolean rich ;  // 该区域是否充足

    public int totalGoodNum;
    public int totalGoodValue;
    public int waitTime;

    /**
     *
     * @param berth
     * @param append 是否在berth区域后面自动划分新区域
     */
    public BerthArea(Berth berth, boolean append) {
        this.berth = berth;
        if (append){
            appendAssign(berth);
        }
    }

    private void appendAssign(Berth berth) {
        // 在berth已分区域后面追加新区域
        level = berth.myAreas.size()+1;
        RegionValue regionValue = berth.staticValue.get(level);
        if (regionValue == null){
            Util.printErr("appendAssign： 没有更多区域");
            return;
        }
        expGoodNum = regionValue.getGoodNum();
        expMaxStep = regionValue.getExpStep();
        expMaxStepNum = regionValue.getExpMaxStepNum();
        rich = regionValue.isAreaRich();
        if (level > 1){
            RegionValue last = berth.staticValue.get(level - 1);    // 如果没有，值为null
            expGoodNum -= last.getGoodNum();
        }
    }


    public double getExpGoodNum() {
        // 获取机器人一个周期在此区域预期装载的货物
        return expGoodNum;
    }

    public int getLevel() {
        return level;
    }

    public int getWorkTime() {
        return workFps;
    }

    public double getExpMinValue() {
        // 最远的距离即为最小价值
        return Const.avgGoodValue/(expMaxStep*2);
    }

    public int getExpMaxStep() {
        return expMaxStep;
    }

    public int getExpMaxStepNum() {
        return expMaxStepNum;
    }

    public boolean isRich() {
        return rich;
    }

    @Override
    public String toString() {
        return "BerthArea{" +
                "berth=" + berth +
                "robot=" + (robot!=null?robot.id:-1) +
                ", single=" + single +
                ", level=" + level +
                ", workFps=" + workFps +
                ", expGoodNum=" + expGoodNum +
                ", expMaxStep=" + expMaxStep +
                ", expMaxStepNum=" + expMaxStepNum +
                ", rich=" + rich +
                '}';
    }

    public void setCombineValue(double expGoodNum, int expMaxStep, int workFps) {
        this.expGoodNum = expGoodNum;
        this.expMaxStep = expMaxStep;
        this.workFps = workFps;
        this.single = false;
        this.expMaxStepNum = 0;
        this.rich = expMaxStep < berth.staticValue.get(berth.staticValue.size()).getExpStep()-2;
    }

    public void enable(Robot robot) {
        // 这个区域正式被机器人划分走
        this.robot = robot;
        level = berth.myAreas.size() + 1;
        double minValue = getExpMinValue();
        for (Good good : berth.domainGoodsByValue) {
            if (good.fpsValue > minValue){
                areaGoodsByTime.add(good);
                // 将所有满足的物品都加上
            }
        }
        berth.myAreas.add(this);
    }
}
