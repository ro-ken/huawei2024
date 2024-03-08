package com.huawei.codecraft.core;

import com.huawei.codecraft.Const;
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
        removeGoods(useless);
        return target;
    }

    private void removeGoods(ArrayList<Good> useless) {

    }

    // 时间不够了
    private boolean timeNotEnough(Good good) {
        return false;
    }

    public void setBook(Good good) {
        bookGoods.add(good);
    }


}

