package com.huawei.codecraft.util;

import com.huawei.codecraft.Util;
import com.huawei.codecraft.core.Berth;
import com.huawei.codecraft.core.Boat;

import java.util.ArrayList;
import java.util.Collections;

import static com.huawei.codecraft.Const.*;

public class BoatPath {
    public Boat boat;
    // 轮船的路径
    public ArrayList<Berth> myPath = new ArrayList<>();
    int pathNum = 1;        // 路径数量
    int index = 0;     // 始终指向下一个节点
    int totalFps;       // 所有路径加一起的大周期
//    boolean needChange=false;     // 是否需要转换顺序
    public int minT; // 最小完整周期，最后一个阶段的调度周期
    public int normalT;    // 前面正常周期  total = normalT * n + minT ;
    private int lastTFPS=0;  // 最迟进入周期时间
    private int startFrame; // 周期开始时间
    public Point delivery;
    private int firstBerthToEndFps;
    public boolean lastPeriod = false;
    public ArrayList<Integer> fpsSeq = new ArrayList<>();
    public ArrayList<Integer> realSeq = new ArrayList<>();
    public ArrayList<Integer> sizeSeq = new ArrayList<>();

    public BoatPath(ArrayList<Berth> path, int period) {
        // 只有一条路径   ,没有boat，不启用
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
        assignMyPath(path.getObj1());
        totalFps = path.getObj2();
        startFrame = frameId;
        delivery = myPath.get(myPath.size()-1).getClosestDelivery();    // 交货点选离最后节点近的，离交货点近的要放最后一个
        updateTime();

    }

    private void assignMyPath(ArrayList<Berth> path) {
        myPath.addAll(path);
        // 要求最后一个点是里泊口最近的点
        if (path.get(0).getClosestDeliveryFps() < path.get(path.size()-1).getClosestDeliveryFps()){
            Collections.reverse(myPath);
        }
    }

    private void assignDelivery() {
        // 确定交货点，一个就行
        if (myPath.get(0).getClosestDeliveryFps() < myPath.get(myPath.size()-1).getClosestDeliveryFps()){
            delivery = myPath.get(0).getClosestDelivery();
        }else {
            delivery = myPath.get(myPath.size()-1).getClosestDelivery();
        }
    }

    private void updateTime() {
        // 更新周期时间，交货点一个就行
        int t0 = myPath.get(0).getSeaPathFps(delivery);  // 可能会有几帧偏差，正常delivery -> 0；
        firstBerthToEndFps = 0;
        if (myPath.size() >1){
            for (int i = 0; i < myPath.size() - 1; i++) {
                firstBerthToEndFps += myPath.get(i).getSeaPathFps(myPath.get(i+1).core);
            }
        }
        firstBerthToEndFps += myPath.get(myPath.size() - 1).getSeaPathFps(delivery);

        minT = t0 + firstBerthToEndFps + Math.min(Boat.capacity,80);    // 确定最后一个周期时间，
        lastTFPS = totalFrame - minT;
        // 确定前面周期时间 Total = n*T1 + minT;
        clacNormalT(lastTFPS-frameId);
        Util.printLog(boat+"最小周期："+minT+"普通周期："+normalT);
    }

    private void clacNormalT(int left) {
        for (int i = 1; i <= 100; i++) {
            int t = left / i;
            if (t >= minT){
                normalT = t;
            }else {
                break;
            }
        }

        int total = lastTFPS;
        while (true){
            fpsSeq.add(0,total);
            if (total - normalT > frameId){
                total -= normalT;
            }else {
                break;
            }
        }
        Util.printDebug(boat+"序列："+fpsSeq);
    }

//    private void setStatus(Boat boat) {
//        // 动态根据boat的状态来设置此路径
//        if (boat.status == BoatStatus.LOAD || boat.status == BoatStatus.SHIP){
//            for (int i = 0; i < myPath.size(); i++) {
//                if (myPath.get(i) == boat.bookBerth){
//                    pathIndex = i+1;
//                }
//            }
//        }
//    }

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
        if (index < myPath.size()){
            next = myPath.get(index++);
        }
        if (boat.leftCapacity()<3){
            next = null;
        }
        if (next != null){
           return new Twins<>(next,null);
        }else {
//            // 需要去交货点
//            if (needChange){
//                Collections.reverse(myPath);
//            }
            index=0;
            return new Twins<>(null,delivery);
        }
    }

    private Twins<Berth, Point> getNextPlaceTwoPath() {
        return null;
    }

    public boolean needGoNormal() {
        // 普通周期是否进入下一个点
        int size = 0;
        int t = 0;
        for (int i = index; i < myPath.size(); i++) {
            size += myPath.get(i).existGoods.size();
            t += myPath.get(i-1).getSeaPathFps(myPath.get(i).core);
        }
        t += myPath.get(myPath.size()-1).getSeaPathFps(delivery);


        int num = myPath.size()-index;
        int left = Boat.capacity - boat.carry - size;
        if (left < num * 2) return true;  // 货满了赶紧走

        // 且预估到第二个点的时间
        int end = getClosestNormalEndT();
        int needGo = end - t - (Boat.capacity-boat.carry);
        if (frameId > needGo){
            return true;
        }
        return false;
    }


    public int getClosestNormalEndT() {
        // 得到下一个最近普通周期的末尾FPS
        int res = lastTFPS;
        while (true){
            if (res - normalT > frameId){
                res -= normalT;
            }else {
                break;
            }
        }
        return res;
    }


    public boolean canIntoLastPeriod() {
        // 去一个泊口装货时间不够往返，进入，否则不进
//        int b2x = Math.min(berths.get(0).transport_time,berths.get(1).transport_time);
        if (frameId >= lastTFPS - normalT + Math.min(100,normalT)){
            return true;
        }
        return false;
    }

    public void setDeadLine(Berth berth) {
        // 设置到期时间 = 这个泊口离开到delivery的时间
        int t = 0;
        for (int i = index; i < myPath.size(); i++) {
            t += myPath.get(i-1).getSeaPathFps(myPath.get(i).core);
        }
        if (index <= myPath.size()-1){
            t += myPath.get(myPath.size()-1).getSeaPathFps(delivery);
        }
        int deadLine = totalFrame - t - boat.leftCapacity();    // todo 这个时间可以在算精确一点

        berth.setDeadLine(deadLine);    // 运输时间 + 装载时间
    }

    public boolean timeNotEnoughTo(Berth berth) {
        // 时间不足以去这个泊口
        int fps = berth.getSeaPathFps(boat.pos) + berth.getClosestDeliveryFps() + 3;
        return frameId > totalFrame - fps;
    }

    public boolean isLastBerth(Berth berth) {
        return berth == myPath.get(myPath.size()-1);
    }
}
