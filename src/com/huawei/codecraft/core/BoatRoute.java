package com.huawei.codecraft.core;

import com.huawei.codecraft.Const;
import com.huawei.codecraft.Util;
import com.huawei.codecraft.util.Point;

import java.util.ArrayList;
import java.util.List;

public class BoatRoute {
    Boat boat;
    public Point target;    // 要抵达的目标
    public List<Point> way = new ArrayList<>();
    public int index=0;
    public BoatRoute(Boat boat){
        this.boat = boat;
        setSelfWay();
    }

    // 更新机器人路由
    private void setWay(List<Point> path) {
        way = path;
        target = way.get(way.size()-1);
        index=0;
        boat.next = getNextPoint();
    }
    private void setSelfWay() {
        // 只有一个点，机器人原地待命
        List<Point> path = new ArrayList<>();
        path.add(new Point(boat.pos));
        setWay(path);
    }

    public void setNewWay(Point pos){
        if (pos.equals(boat.pos)){
            // 原地待命
            setSelfWay();
            return;
        }

        long sta = System.nanoTime();
        // 没有保存路径，自己寻路
        ArrayList<Point> path = Const.path.getBoatPath(boat.pos,boat.direction,pos);
        long end = System.nanoTime();
        if (path == null){
            // 后续判断，如果target!=pos说明找不到路
            Util.printErr("boat setNewWay:找不到路"+boat.pos +"->"+pos);
            setSelfWay();
        }else {
            Util.printLog("boat重新寻路！ 距离："+path.size()+"花费时间:"+(end-sta)/1000+"us");
            setWay(path);
        }
    }
    public void setNewWay(List<Point> path) {
        Util.printLog(boat+"新路径："+path);
        if (path != null){
            setWay(path);
        }else {
            setSelfWay();
            Util.printErr("setNewWay，传入path为null，机器人位置："+boat.pos);
        }
    }

    public Point getNextPoint() {
        if (index >= way.size()-1){
            return target;
        }else {
            Point next = way.get(index++);
            if (next.equals(boat.pos)){
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
        if (index >=2 && boat.pos.equals(way.get(index-2))){
            index --;
        }
    }

    public int leftPathLen() {
        return way.size() - index;
    }

    public List<Point> getLeftPath() {
        // 获取剩余路径
        if (index <= 1){
            return way;
        }else {
            return new ArrayList<>(way.subList(index-2, way.size()));
        }
    }
}
