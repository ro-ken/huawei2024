package com.huawei.codecraft.way;

import com.huawei.codecraft.util.Point;

public class Pos {
    public Point pos;
    public Pos father;  // 注意，father 应该是 Pos 类型

    public int g;       // 从起点到当前点的成本
    public int h;       // 从当前点到终点的启发式估计成本

    // 构造方法也需要相应地调整，以接受g和h作为参数
    public Pos(Point pos, Pos father, int g, int h) {
        this.pos = pos;
        this.father = father;
        this.g = g;
        this.h = h;
    }

    // 一个方法来计算f值
    public int f() {
        return g + h;
    }
}
