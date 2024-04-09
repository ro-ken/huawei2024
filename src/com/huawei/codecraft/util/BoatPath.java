package com.huawei.codecraft.util;

import com.huawei.codecraft.core.Berth;

import java.util.ArrayList;

public class BoatPath {
    // 轮船的路径
    ArrayList<ArrayList<Berth>> paths = new ArrayList<>();
    int pathNum = 1;        // 路径数量
    int pathIndex = 0;      // 路径下标
    int berthIndex = 0;     // 路径内泊口的下标
    int totalFps;       // 所有路径加一起的大周期

    public BoatPath(ArrayList<Berth> path, int period) {
        // 只有一条路径
        paths.add(path);
        totalFps = period;
    }

    @Override
    public String toString() {
        return "BoatPath{" +
                ", pathNum=" + pathNum +
                ", totalFps=" + totalFps +
                "paths=" + paths +
                '}';
    }
}
