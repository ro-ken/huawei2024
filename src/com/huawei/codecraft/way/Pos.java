package com.huawei.codecraft.way;

import com.huawei.codecraft.util.Point;

public class Pos {
    public Point pos;
    public Pos father;  // 注意，father 应该是 Pos 类型

    public Pos(Point p) {
        this.pos = p;
        this.father = null;
    }

    public Pos(Point pos, Pos father) {
        this.pos = pos;
        this.father = father;
    }
}
