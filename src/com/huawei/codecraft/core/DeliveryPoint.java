package com.huawei.codecraft.core;

import com.huawei.codecraft.util.Point;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class DeliveryPoint {
    public  Point pos;
    public  List<HashSet<Point>> deliveryRectangleAreaPoints = new ArrayList<>(); // 出发拥有的矩形区域

    public DeliveryPoint(Point pos) {
        this.pos = pos;
    }
}
