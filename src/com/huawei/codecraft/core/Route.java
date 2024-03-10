package com.huawei.codecraft.core;

import com.huawei.codecraft.Const;
import com.huawei.codecraft.Util;
import com.huawei.codecraft.util.Point;

import java.util.ArrayList;

public class Route {

    Robot robot;
    public Point target;    // 要抵达的目标
    public ArrayList<Point> way = new ArrayList<>();
    public int index=0;
    public Route(Point pos,Robot robot){
        this.robot = robot;
        setNewWay(pos);
    }

    private void setWay(ArrayList<Point> path) {
        way = path;
        target = way.get(way.size()-1);
        index=0;
        if (way.get(0).equals(robot.pos)){
            // 让next点指向下一位
            robot.updateNextPoint();
        }
    }

    // 只有一个点，机器人原地待命
    private void setWay(Point robotPos) {
        target = new Point(robotPos);
        way = new ArrayList<>();
        way.add(robotPos);
        index=0;
    }
    public void setNewWay(Point pos){
        if (pos.equals(robot.pos)){
            // 原地待命
            setWay(robot.pos);
        }
        else {
            // 寻路,找不到路，为null
            ArrayList<Point> path = Const.path.getPath(robot.pos,target);
            if (path == null){
                // 后续判断，如果target!=pos说明找不到路
                setWay(robot.pos);
            }else {
                setWay(path);
            }
        }
    }
    public void setNewWay(ArrayList<Point> path) {
        setWay(path);
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
    public Point getLastPoint() {
        // 去上一个点
        index = Math.max(0,index-2);
        return way.get(Math.max(0,index-1));
    }

    public void stayCurPoint() {
        if (index >=2 && robot.pos.equals(way.get(index-2))){
            index --;
        }
    }

    public int leftPathLen() {
        return way.size() - index;
    }

    public ArrayList<Point> getLeftPath() {
        // 获取剩余路径
        if (index == 0){
            return way;
        }else {
            return (ArrayList<Point>) way.subList(index-1,way.size());
        }
    }


}
