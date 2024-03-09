package com.huawei.codecraft.core;

import com.huawei.codecraft.Const;
import com.huawei.codecraft.Util;
import com.huawei.codecraft.util.Pair;
import com.huawei.codecraft.util.Point;

import java.util.*;

// 泊位
public class Berth {
    public int id;
    public Point pos;
    public int transport_time;
    public int loading_speed;
    public PriorityQueue<Pair<Good>> goodList = new PriorityQueue<>(30);
    public Set<Good> bookGoods = new HashSet<>();
    public Set<Boat> bookBoats = new HashSet<>();
    public Deque<Good> existGoods = new LinkedList<>();     // 泊口存在的货物
    public int existValue=0;           // 泊口货物总价值

    public Berth(int id) {
        pos = new Point();
        this.id = id;
    }

    public Berth(int x, int y, int transport_time, int loading_speed) {
        this.pos = new Point(x,y);
        this.transport_time = transport_time;
        this.loading_speed = loading_speed;
    }

    //将每帧新加的货物更新自己列表
    public void updateGoodList(ArrayList<Good> frameGoods) {
        for (Good good:frameGoods){
            double fps = Const.path.getPathFps(pos,good.pos) * 2;   // 一个来回
            double cost = fps/good.value;
            Pair<Good> pair = new Pair<>(good,cost);
            goodList.add(pair);
        }
    }
    // 取最佳货物
    public Good getBestGood(){
        ArrayList<Good> useless = new ArrayList<>();// 无效货物
        Good target = null;
        for (Pair<Good> pair : goodList) {
            Good good = pair.getObject();
            if (timeNotEnough(good)){
                useless.add(good);
                continue;
            }
            if (good.isNotBook()){
                // 未被预定，可选
                target = good;
                break;
            }else {
                useless.add(good);
            }
        }
        removeBookGoods(useless);
        return target;
    }

    private void removeBookGoods(ArrayList<Good> useless) {

    }

    @Override
    public String toString() {
        return "Berth{" +
                "id=" + id +
                ", pos=" + pos +
                ", transport_time=" + transport_time +
                ", loading_speed=" + loading_speed +
                '}';
    }

    // 时间不够了
    private boolean timeNotEnough(Good good) {
        return false;
    }

    public void setBook(Good good) {
        bookGoods.add(good);
    }

    public void addGood(Good good){
        bookGoods.remove(good);
        existGoods.add(good);
        existValue += good.value;
    }
    public void removeGood(){
        Good good = existGoods.pop();
        existValue -= good.value;
        if (existValue <0){
            existValue = 0;
            Util.printLog("ERROR! existValue <0");
        }
    }

    // 移出货物
    public void removeGoods(int size) {
        for (int i = 0; i < size; i++) {
            removeGood();
        }
    }

    public void addBoat(Boat boat) {
        bookBoats.add(boat);
    }
}
