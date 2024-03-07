package com.huawei.codecraft.core;

//机器人
public class Robot {
    public int id;
    public int x, y, goods;
    public int status;
    public int mbx, mby;

    public Robot(int id) {
        this.id = id;
    }

    public Robot(int startX, int startY) {
        this.x = startX;
        this.y = startY;
    }
}