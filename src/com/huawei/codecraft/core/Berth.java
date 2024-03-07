package com.huawei.codecraft.core;

import com.huawei.codecraft.util.Pos;

// 泊位
public class Berth {
    public int id;
    public Pos pos;
    public int transport_time;
    public int loading_speed;

    public Berth(int id) {
        pos = new Pos();
        this.id = id;
    }

    public Berth(int x, int y, int transport_time, int loading_speed) {
        this.pos = new Pos(x,y);
        this.transport_time = transport_time;
        this.loading_speed = loading_speed;
    }
}

