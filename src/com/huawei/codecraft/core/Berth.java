package com.huawei.codecraft.core;

import com.huawei.codecraft.Const;
import com.huawei.codecraft.Main;
import com.huawei.codecraft.Util;
import com.huawei.codecraft.util.Pair;
import com.huawei.codecraft.util.Point;
import com.huawei.codecraft.zone.Region;

import java.util.*;

// 泊位
public class Berth {
    public int id;
    public Point pos;
    public int transport_time;
    public int loading_speed;
    public Region region;  // 该泊口属于的区域，在区域初始化赋值
    public PriorityQueue<Pair<Good>> goodList = new PriorityQueue<>();
    public PriorityQueue<Pair<Good>> domainGoodsByValue = new PriorityQueue<>();  // 需要被运输的货物,按照单位价值排序
    public Deque<Good> domainGoodsByTime = new LinkedList<>();      // 需要被运输的货物,按照时间先后排序
    public Set<Boat> bookBoats = new HashSet<>();
    public Deque<Good> existGoods = new LinkedList<>();     // 泊口存在的货物
    public int existValue=0;           // 泊口货物总价值
    public Map<Point,List<Point>> mapPath = new HashMap<>();   //  地图所有点到该泊位的路径信息
    public int totalGoodNum;

    public Berth(int id) {
        pos = new Point();
        this.id = id;
    }

    public void assignRegion(Region region){
        this.region = region;
    }

    public Berth(int x, int y, int transport_time, int loading_speed) {
        this.pos = new Point(x,y);
        this.transport_time = transport_time;
        this.loading_speed = loading_speed;
    }

    public Pair<Good> calcGoodValue(Good good) {
        // 上层判断非空再传下来
        double fps = getPathFps(good.pos)*2;    // 一个来回
        double value = good.value/fps;
        Pair<Good> pair = new Pair<>(good,value);
        return pair;
    }

    public int getPathFps(Point pos) {
//        Util.printDebug(mapPath);
        if (mapPath.containsKey(pos)){
            return mapPath.get(pos).size();
        }else {
            return Const.unreachableFps;
        }
    }

    @Override
    public String toString() {
        return "berth{" +
                "id=" + id +
                ", pos=" + pos +
//                ", transport_time=" + transport_time +
//                ", loading_speed=" + loading_speed +
                '}';
    }

    // 增加码头的货物
    public void addBerthGood(Good good){
        existGoods.add(good);
        existValue += good.value;
    }
    public void removeGood(){
        Good good = existGoods.pop();
        existValue -= good.value;
        Main.totalValue += good.value;
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

    public boolean  inMyPlace(Point pos) {
        return pos.x >= this.pos.x-1 && pos.x <= this.pos.x + 2 && pos.y >= this.pos.y-1 && pos.y <= this.pos.y + 2;
    }


    public int expectLoadTime(int t0) {
        // 在T0的时间内预计产生的货物需要装多长时间
        // todo
        return 20;
    }

    public int getPredictGoodNum(int time) {
        // 从现在开始 持续time时间，返回预计有多少货物
        // todo
        return existGoods.size() + 10;
    }


    public boolean canCarryGood(Good good) {
        // 计算能否去取该货物，默认机器人在泊口
        int fps = getPathFps(good.pos);
        // todo 到时候避让得根据所剩余时间计算
        if (fps <= good.leftFps() - 3 && good.isNotBook()){
            // 加几帧弹性时间，怕绕路
            return true;
        }
        return false;
    }

    public void removeDomainGood(Good good) {
        // 清除管理区域的货物,统一删除region中的
        Pair<Good> tar = null;
        for (Pair<Good> pair : domainGoodsByValue) {
            if (pair.getKey() == good){
                tar = pair;
                break;
            }
        }
        if (tar == null){
            Util.printErr("removeDomainGood");
            return;
        }
        removeDomainGood(tar);
    }

    public void removeDomainGood(Pair<Good> pair) {
        Good good = pair.getKey();
        domainGoodsByTime.remove(good);
        domainGoodsByValue.remove(pair);
        region.regionGoodsByTime.remove(good);
        region.regionGoodsByValue.remove(pair);
    }
}

