package com.huawei.core;

//机器人
public class Robot {
    public int x, y, goods;
    public int status;
    public int mbx, mby;

    public Robot() {
    }

    public Robot(int startX, int startY) {
        this.x = startX;
        this.y = startY;
    }
}