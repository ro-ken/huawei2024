package com.huawei.codecraft.core;

// 轮船
public class Boat {
    public int id;
    // 目标泊位，如果目标泊位是虚拟点，则为-1
    public int berthId;
    // 状态：0移动；1正常运行（(即装货状态或运输完成状态）；2表示泊位外等待状态
    public int status;

    public Boat(int id) {
        this.id = id;
    }

    public void schedule() {

    }
}