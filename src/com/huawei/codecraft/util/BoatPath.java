package com.huawei.codecraft.util;

import com.huawei.codecraft.core.Berth;
import com.huawei.codecraft.core.Boat;

import java.util.ArrayList;
import java.util.Collections;

public class BoatPath {
    public Boat boat;
    // 轮船的路径
    public ArrayList<Berth> myPath = new ArrayList<>();
    int pathNum = 1;        // 路径数量
    int pathIndex = 0;      // 路径下标
    int berthIndex = 0;     // 路径内泊口的下标
    int totalFps;       // 所有路径加一起的大周期
    boolean needChange=false;     // 是否需要转换顺序

    public BoatPath(ArrayList<Berth> path, int period) {
        // 只有一条路径
        myPath.addAll(path);
        totalFps = period;
    }

//    public BoatPath(ArrayList<Berth> path1, ArrayList<Berth> path2) {
//        Twins<ArrayList<Berth>, Integer> tw1 = Boat.getSinglePathAndFpsByEnum(path1);
//        Twins<ArrayList<Berth>, Integer> tw2 = Boat.getSinglePathAndFpsByEnum(path2);
//        paths.add(tw1.getObj1());
//        paths.add(tw2.getObj1());
//        pathNum = 2;
//        totalFps = tw1.getObj2() + tw2.getObj2();
//    }

    public BoatPath(Twins<ArrayList<Berth>, Integer> path, Boat boat) {
        this.boat = boat;
        myPath.addAll(path.getObj1());
        totalFps = path.getObj2();
        Point d1 = myPath.get(0).getClosestDelivery();
        Point d2 = myPath.get(myPath.size()-1).getClosestDelivery();
        if (!d1.equals(d2)){
            needChange = true;
        }
        setStatus(boat);
    }

    private void setStatus(Boat boat) {
        // 动态根据boat的状态来设置此路径
        if (boat.status == BoatStatus.LOAD || boat.status == BoatStatus.SHIP){
            for (int i = 0; i < myPath.size(); i++) {
                if (myPath.get(i) == boat.bookBerth){
                    pathIndex = i+1;
                }
            }
        }
    }

//    public void enable(Boat boat) {
//        // 将次路径启动轮船
//        this.boat = boat;
//
//        Point d1 = myPath.get(0).getClosestDelivery();
//        Point d2 = myPath.get(myPath.size()-1).getClosestDelivery();
//        if (!d1.equals(d2)){
//            needChange = true;
//        }
//    }
    @Override
    public String toString() {
        return "BoatPath{" +
                ", pathNum=" + pathNum +
                ", totalFps=" + totalFps +
                ",myPath=" + myPath +
                '}';
    }

    public Twins<Berth, Point> getNextPlace() {
        Twins<Berth, Point> twins;
        if (pathNum == 1){
            twins = getNextPlaceOnePath();
        }else {
            twins = getNextPlaceTwoPath();
        }
        return twins;
    }

    private Twins<Berth, Point> getNextPlaceOnePath() {
        Berth next = null;
        if (berthIndex < myPath.size()){
            //
            next = myPath.get(berthIndex++);
        }
        if (boat.leftCapacity()<3){
            next = null;
        }
        if (next != null){
           return new Twins<>(next,null);
        }else {
            Point delivery = myPath.get(myPath.size()-1).getClosestDelivery();
            // 需要去交货点
            if (needChange){
                Collections.reverse(myPath);
            }
            berthIndex=0;
            return new Twins<>(null,delivery);
        }
    }

    private Twins<Berth, Point> getNextPlaceTwoPath() {
        // todo
        return null;
    }
}
