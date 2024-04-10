package com.huawei.codecraft.core;

import com.huawei.codecraft.Const;
import com.huawei.codecraft.Util;
import com.huawei.codecraft.util.Point;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Route {

    Robot robot;
    public Point target;    // 要抵达的目标
    public List<Point> way = new ArrayList<>();
    public int index=0;
    public Route(Robot robot){
        this.robot = robot;
        setSelfWay();
    }

    private void setWay(List<Point> path) {
        way = path;
        target = way.get(way.size()-1);
        index=0;
        robot.next = getNextPoint();
    }


    private void setSelfWay() {
        // 只有一个点，机器人原地待命
        List<Point> path = new ArrayList<>();
        path.add(new Point(robot.pos));
        setWay(path);
    }
    public void setNewWay(Point pos){
        if (pos.equals(robot.pos)){
            // 原地待命
            setSelfWay();
            return;
        }
        if (Const.pointToBerth.containsKey(pos)){
            // 该点是泊位的距离
            Berth berth = Const.pointToBerth.get(pos);
            List<Point> path = berth.getMinLandPath(robot.pos);
//            Util.printLog("setNewWay1:berth"+berth.pos+"pos"+robot.pos+"path"+path);
            if (path != null){
                List<Point> path1 = new ArrayList<>(path);
                Collections.reverse(path1);  // 保存的是泊位到点的路径，需要翻转
                setNewWay(path1);
                return;
            }
        }else if (Const.pointToBerth.containsKey(robot.pos)){
            // 机器人所在的点是泊位的点
            Berth berth = Const.pointToBerth.get(robot.pos);
            List<Point> path = berth.landMapPath.get(robot.pos).get(pos);
//            Util.printDebug("setNewWay2:berth"+berth.pos+"pos"+robot.pos+"path"+path);
            if (path != null){
                setNewWay(path);
                return;
            }
        }else if (Const.landHotPath.containsKey(robot.pos)){
            // 机器人所在的点是热路径
            List<Point> path = Const.landHotPath.get(robot.pos).get(pos);
//            Util.printDebug("setNewWay2:berth"+berth.pos+"pos"+robot.pos+"path"+path);
            if (path != null){
                setNewWay(path);
                return;
            }
        }


        long sta = System.nanoTime();
        // 没有保存路径，自己寻路
        ArrayList<Point> path = Const.path.getPath(robot.pos,pos);
        long end = System.nanoTime();
        if (path == null){
            // 后续判断，如果target!=pos说明找不到路
            Util.printErr("setNewWay:找不到路"+robot.pos +"->"+pos);
            setSelfWay();
        }else {
            Util.printLog("未保存路径，重新寻路！ 距离："+path.size()+"花费时间:"+(end-sta)/1000+"us");
            setWay(path);
        }
    }
    public void setNewWay(List<Point> path) {
        Util.printLog(robot+"新路径："+path);
        if (path != null){
            setWay(path);
        }else {
            setSelfWay();
            Util.printErr("setNewWay，传入path为null，机器人位置："+robot.pos);
        }
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

    public List<Point> getLeftPath() {
        // 获取剩余路径
        if (index <= 1){
            return way;
        }else {
            return new ArrayList<>(way.subList(index-2, way.size()));
        }
    }
}
