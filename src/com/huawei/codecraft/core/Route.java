package com.huawei.codecraft.core;

import com.huawei.codecraft.Const;
import com.huawei.codecraft.Util;
import com.huawei.codecraft.util.Point;

import java.util.ArrayList;

public class Route {

    Robot robot;
    public Point target;    // 要抵达的目标
    public ArrayList<Point> way;
    public int index=0;
    public Route(Point pos,Robot robot){
        target = new Point(pos);
        this.robot = robot;
        // 寻路,找不到路，为null
        way = Const.path.getPath(robot.pos,target);
    }

    public Point peekNextPoint(){
        return way.get(index);
    }

    public Point getNextPoint() {
        if (index >= way.size()-1){
            return target;
        }else {
            Point next = way.get(index++);
            if (next.equals(robot.pos)){
                next = way.get(index++);   // 是自己的点，去下一个点
            }
            return next;
        }
    }
}
