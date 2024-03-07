package com.huawei.codecraft.core;


import com.huawei.codecraft.util.Pos;

// 货物类
public class Good {
    // 位置
    public Pos pos;
    // 物品消失帧数
    public int deadFrame;
    private static final int maxSurvive = 20*50;
    // 价值
    public int value;

    public Good(int x,int y, int value,int curFrame) {
        this.pos = new Pos(x,y);
        this.value = value;
        // todo 当前帧是否算进去？
        deadFrame = curFrame + maxSurvive-1;
    }
}
