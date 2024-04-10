package com.huawei.codecraft.core;


import com.huawei.codecraft.Const;
import com.huawei.codecraft.util.Point;

// 货物类
public class Good {
    // 位置
    public Point pos;
    // 物品消失帧数
    public int deadFrame;
    public static final int maxSurvive = 20*50;
    // 价值
    public int value;
    public double fpsValue;



    public Good(int x,int y, int value,int curFrame) {
        this.pos = new Point(x,y);
        this.value = value;
        deadFrame = curFrame + maxSurvive-1;
    }

    @Override
    public String toString() {
        return "Good{" +
                "pos=" + pos +
                ", value=" + value +
                '}';
    }

    // 该物品是否存在，
    public boolean isExist() {
        return deadFrame >= Const.frameId;
    }

    public int leftFps() {
        return deadFrame - Const.frameId;
    }

    public void setFpsValue(int dis) {
        fpsValue = (double) value /dis;  // 单位时间价值
    }
}
