package com.huawei.core;

// 泊位
public class Berth {
    public int x;
    public int y;
    public int transport_time;
    public int loading_speed;

    public Berth() {
    }

    public Berth(int x, int y, int transport_time, int loading_speed) {
        this.x = x;
        this.y = y;
        this.transport_time = transport_time;
        this.loading_speed = loading_speed;
    }
}

