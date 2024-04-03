package com.huawei.codecraft.core;

import com.huawei.codecraft.Const;
import com.huawei.codecraft.Main;
import com.huawei.codecraft.Util;
import com.huawei.codecraft.util.BoatLastTask;
import com.huawei.codecraft.util.BoatStatus;
import com.huawei.codecraft.util.Point;


import java.util.*;

import static com.huawei.codecraft.Const.*;

// 轮船
public class Boat {
    public int id;
    public int readsts;    // 状态：0正常行驶；1恢复状态；2装载状态

    public Point pos;
    public int direction;   // 机器人当前朝向
    BoatStatus status=BoatStatus.FREE;
    public static int capacity;
    public int carry;    // 携带物品数量
    public Berth bookBerth;
    Point next;
    public int startFrame;
    public int goodSize;
    BoatRoute route;
    BoatLastTask task ;

    public Boat(int id,Point p) {
        this.id = id;
        pos = new Point(p);
        route = new BoatRoute(this);      // todo 记得打开
    }

    public static void handleBoatMove() {
        for (Boat boat : boats) {
            boat.printMove();
        }
    }

    private void printMove() {
        if (status == BoatStatus.SHIP || status == BoatStatus.GO){
            int dis = next.clacGridDis(pos);
            Util.printLog("move:" + this);
            if (dis == 0)  {
                Util.printWarn("船位置重合！");
            }else  if (dis == 1) {
                // 1为前进
                Util.boatShip(id);
                Util.printLog("boat前进");
            }else if (dis == 2){
                if (Math.abs(next.x-pos.x)==1){
                    // 逆时针，下一个点在对角
                    Util.boatAnticlockwise(id);
                    Util.printLog("boat左转");
                }else {
                    // 顺时针
                    Util.boatClockwise(id);
                    Util.printLog("boat右转");
                }
            }else {
                Util.printErr("下一个坐标点有误");
            }
        }
    }

    private boolean isAntiClockwise() {

        return false;
    }

    private boolean isClockwise() {
        // 是否顺时针转
        // 是否向前
        if (direction == RIGHT){
            return next.y - pos.y == 1;
        }else if (direction == LEFT){
            return next.y - pos.y == -1;
        }else if (direction == UP){
            return next.x - pos.x == -1;
        }else if (direction == DOWN){
            return next.x - pos.x == 1;
        }
        return false;
    }

    private boolean isForward() {
        // 是否向前

        if (direction == RIGHT){
            return next.y - pos.y == 1;
        }else if (direction == LEFT){
            return next.y - pos.y == -1;
        }else if (direction == UP){
            return next.x - pos.x == -1;
        }else if (direction == DOWN){
            return next.x - pos.x == 1;
        }
        return false;
    }

    public void schedule() {
        simpleSched();
    }

    private void simpleSched() {
        // 轮船简单调度，在各大泊口间轮转，满了卸货
        if (inRecoverMode()){
            return;     // 恢复状态不能操作
        }
        if (status != BoatStatus.FREE){
            handleBoatTask();
        }
        if (status == BoatStatus.FREE){
            // 没有任务
            bookBerth = selectHighValueBerth();
            status = BoatStatus.SHIP;
            changeRoad(bookBerth.pos);
        }
    }

    private void handleBoatTask() {
        if (status == BoatStatus.SHIP){
            // 驶向泊口状态
            if (isArriveBerthArea()){
                Util.printLog(this+"boat arrive："+bookBerth);
                Util.boatBerth(id);
                status = BoatStatus.LOAD;   // 可以测试一下是否能立马泊靠
            }
        }else if (status == BoatStatus.LOAD){
            if (startFrame == 0){
                startFrame = frameId;
            }
            if (isLoadFinish()){
                Util.printLog("搬运结束");
                clacGoods();//结算货物
                goToDelivery();
                status = BoatStatus.GO;
                startFrame = 0;
            }
        }else if(status == BoatStatus.GO){
            if (isArriveDelivery()){
                resetBoat();        // 重置船
                // 需要判断是否进入最后周期
                status = BoatStatus.FREE;
            }
        }
    }

    // 换新的路
    public void changeRoad(Point target) {
        route.setNewWay(target);
        Util.printLog("boat 寻路："+route.way);
        if (!route.target.equals(target)) {
            Util.printLog(this.pos + "->" + target + ":tar" + route.target);
            Util.printErr("boat 找不到路");
        }
    }
    private boolean isArriveDelivery() {
        // 是否到达交货点
        return boatDeliveries.contains(pos);
    }

    private Berth selectHighValueBerth() {
        // 选择task中价值最高的泊位
        Berth target = berths.get(0);
        int maxVal = target.existValue;

        for (Berth berth : berths) {
            if (!berth.bookBoats.isEmpty()){
                continue;
            }
            if (berth.existValue>maxVal){
                maxVal = berth.existValue;
                target = berth;
            }
        }
        return target;
    }

    private boolean inRecoverMode() {
        return readsts == 1;
    }
    public void updateNextPoint() {
        // 已经在下一个点了，要重新取点，否则不变
        // 2出调用，每帧中间，有新路径
        if (pos.equals(next)) {
            next = route.getNextPoint();
        }
    }
    private void lastPeriodSched() {

        if (status == BoatStatus.LOAD){
            if (mustGotoVirtual()){
                finalGotoVirtual();
            }else {
                if (task.isLastBerth()){
                    if (boatIsFull()){
                        finalGotoVirtual();
                    }
                    return;     // 最后一个泊口，等最后一帧在走
                }
                if (isLoadFinish()){
                    clacGoods();//结算货物
                    Berth berth = task.getNextBerth();  // 换下一个泊口
                    changeBerthAndShip(berth);
                    berth.capacity = capacity - goodSize;
                }
            }
        }
    }

    private void finalGotoVirtual() {
        Util.printDebug(this+"船最后一次调度："+bookBerth);
        Util.printLog("泊口浪费时间:" + (totalFrame - frameId - bookBerth.transport_time) + "船泊停靠时间："+ (frameId-startFrame-1)+"运输时间"+ bookBerth.transport_time);
        clacGoods();//结算货物
        bookBerth.capacity = 0; // 没有船会去装了
        goToDelivery();
    }

    private boolean boatIsFull() {
        int left = capacity - goodSize;
        int loadGoods = Math.min(countGoods(),bookBerth.existGoods.size()); // 容量无限下这段时间装载量
        return left <= loadGoods;   // 实际装载量 > 轮船剩余空位
    }

    private boolean mustGotoVirtual() {
        // 时间不够，必须回虚拟点了,
        if (frameId + bookBerth.transport_time >= totalFrame){
            return true;
        }
        return false;
    }



    private void setDeadLine(Berth berth) {
        // 给这个泊口设定deadLine
        int expSize = (int) (task.getMinT() / Good.maxSurvive * berth.staticValue.get(1).getGoodNum());

        berth.setDeadLine(frameId + berth.transport_time + expSize/berth.loading_speed);    // 运输时间 + 装载时间
    }

    private void resetBoat() {
        goodSize = 0;
    }

    private void loadFinishNextStep() {
        // 装载结束后下一步动作，虚拟点 or 泊口
        Berth next = task.getNextBerth();
        // 计算还能否装下一个泊口的货物
        int nextGood = next.getPredictGoodNum(b2bFps);
        if (capacity >= goodSize + nextGood){
            // 容量够
            if (task.canBerthGotoBerth(next)){
                changeBerthAndShip(next);
                return;
            }
        }
        // 容量不够了，或没时间，先回去
        goToDelivery();
    }

    private void changeBerthAndShip(Berth next) {
        resetBookBerth();
        if (next == null)
            return;
        shipToBerth(next);
    }


    private static ArrayList<Berth> getHighestLowestBerths(List<Berth> berthList) {
        // 得到berthList 价值最高和最低的berth；
        ArrayList<Berth> res = new ArrayList<>();
        if (berthList.isEmpty()) return res;
        Berth high = berthList.get(0);
        Berth low = berthList.get(1);
        for (Berth berth : berthList) {
            if (berth.staticValue.get(1).getGoodNum() > high.staticValue.get(1).getGoodNum()){
                high = berth;
            }
            if (berth.staticValue.get(1).getGoodNum() < low.staticValue.get(1).getGoodNum()){
                low = berth;
            }
        }
        berthList.remove(high);
        berthList.remove(low);
        res.add(high);
        res.add(low);
        return res;
    }

    private static ArrayList<Berth> getClosestTwinsBerth(List<Berth> berthList) {
        // 获取berthList中两个最近的泊口
        ArrayList<Berth> res = new ArrayList<>();
        int min = unreachableFps;
        Berth tar1 = null;
        Berth tar2 = null;
        for (int i = 0; i < berthList.size()-1; i++) {
            for (int j = i+1; j < berthList.size(); j++) {
                int dis = berthList.get(i).getPathFps(berthList.get(j).pos);
                if (dis < min){
                    min = dis;
                    tar1 = berthList.get(i);
                    tar2 = berthList.get(j);
                }
            }
        }
        if (min < unreachableFps){
            berthList.remove(tar1);
            berthList.remove(tar2);
            res.add(tar1);
            res.add(tar2);
        }
        return res;
    }


    // 是否到达了靠泊区
    private boolean isArriveBerthArea() {
        return bookBerth.boatInBerthArea.contains(pos);
    }

    private void findBerthAndShip() {
        resetBookBerth();
        // 寻找berth 并且驶向它
        Berth berth = pickTaskBerth();
        if (berth == null){
            return;
        }
        shipToBerth(berth);
    }
    private void ShipNextBerth() {
        // 寻找berth 并且驶向它
        Berth berth = task.getNextBerth();
        if (berth == null){
            return;
        }
        shipToBerth(berth);
    }

    private void resetBookBerth() {
        if (bookBerth != null){
            bookBerth.bookBoats.remove(this);
            bookBerth = null;
        }
    }

    private void shipToBerth(Berth berth) {
        bookBerth = berth;
        bookBerth.addBoat(this);
        readsts = 0; // 状态转换成移动
        status = BoatStatus.SHIP;
//        Util.printShip(id,bookBerth.id);
    }

    private void goToDelivery() {
        Util.printLog(this+"去交货点");
        resetBookBerth();
        Util.boatDept(id);
        changeRoad(boatDeliveries.get(0));
    }

    private int getRealLoad(){
        // 获得泊口的真实装载量
        int left = capacity - goodSize;
        int loadGoods = Math.min(countGoods(),bookBerth.existGoods.size()); // 容量无限下这段时间装载量
        int realLoad = Math.min(left,loadGoods);    // 实际装载量
        Util.printLog("船的装载："+goodSize+"/"+capacity+"，单次装载量："+realLoad + "，泊口货物："+bookBerth.existGoods.size()+"，装载时间："+(Const.frameId - startFrame - 1));
        return realLoad;
    }

    private void clacGoods() {
        Util.printLog(this+":"+bookBerth+",结算货物：");
        int realLoad = getRealLoad();
        // 互相清算货物
        goodSize += realLoad;
        bookBerth.removeGoods(realLoad);
        for (Berth berth : Const.berths) {
            Util.printLog(berth+"堆积货物"+berth.existGoods.size()+"堆积价值"+berth.existValue);
        }
    }

    private boolean isLoadFinish() {
        int count = countGoods();
        // 没货了或装不下
        if (count >= bookBerth.existGoods.size() || count + goodSize >=capacity){
            // 装完了
            return true;
        }
        else return false;
    }

    // 计算已经装了多少货物
    private int countGoods() {
        int fps = Const.frameId - startFrame - 1;// 当前帧也可以装，后续可以检查
        int count = fps * bookBerth.loading_speed;
        return count;
    }

    // 运输完成，没有任务
    private boolean isFree() {
        // 目标为虚拟点，且到达
        return status == BoatStatus.FREE;
    }

    @Override
    public String toString() {
        return "Boat{" +
                "id=" + id +
                ", rsts=" + readsts +
                ", pos=" + pos +
                ", dire=" + direction +
                ", status=" + status +
                ", next=" + next +
                ", target=" + route.target +
                '}';
    }

    private Berth pickTaskBerth() {
        // 选择task里的泊位,去货物多的

        // 选择task中价值最高的泊位
        Berth target = task.berths.get(0);
        int maxVal = target.existValue;

        for (Berth berth : task.berths) {
            if (!berth.bookBoats.isEmpty()){
                continue;
            }
            if (berth.existValue>maxVal){
                maxVal = berth.existValue;
                target = berth;
            }
        }
        return target;
    }

}