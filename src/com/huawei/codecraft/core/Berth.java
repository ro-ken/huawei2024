package com.huawei.codecraft.core;

import com.huawei.codecraft.Const;
import com.huawei.codecraft.Main;
import com.huawei.codecraft.Util;
import com.huawei.codecraft.util.Pair;
import com.huawei.codecraft.util.Point;
import com.huawei.codecraft.util.RegionValue;
import com.huawei.codecraft.zone.Region;

import java.util.*;

import static com.huawei.codecraft.Const.noLimitedSize;

// 泊位
public class Berth {
    public int id;
    public Point pos;
    public int transport_time;
    public int loading_speed;
    public Region region;  // 该泊口属于的区域，在区域初始化赋值
    public PriorityQueue<Pair<Good>> domainGoodsByValue = new PriorityQueue<>();  // 需要被运输的货物,按照单位价值排序
    public Deque<Good> domainGoodsByTime = new LinkedList<>();      // 需要被运输的货物,按照时间先后排序
    public Set<Boat> bookBoats = new HashSet<>();
    public HashSet<Point> boatInBerthArea = new HashSet<>();    // 靠泊区  todo 需要初始化
    public Deque<Good> existGoods = new LinkedList<>();     // 泊口存在的货物
    public int existValue=0;           // 泊口货物总价值
    public Map<Point,List<Point>> mapPath = new HashMap<>();   //  地图所有点到该泊位的路径信息
    public int deadLine = Const.totalFrame;     // 有效时间，超过这个时间轮船不装了，也不用往这里运了
    public final Map<Integer,Integer> pathLenToNumMap = new HashMap<>();      // 计算区域点到泊口长度对应个数的map
    public Map<Integer, RegionValue> staticValue = new HashMap<>();     // 区域静态价值
    public int points;      // berth拥有的最短路径点个数
    public int capacity = noLimitedSize;
    public int bookGoodSize;

    public void setDeadLine(int deadLine) {
        this.deadLine = deadLine;
    }

    public Berth(int id) {
        pos = new Point();
        this.id = id;
    }

    public void assignRegion(Region region){
        this.region = region;
    }

    public Pair<Good> calcGoodValue(Good good) {
        // 上层判断非空再传下来
        double fps = getPathFps(good.pos)*2;    // 一个来回
        double value = good.value/fps;
        Pair<Good> pair = new Pair<>(good,value);
        return pair;
    }

    public int getPathFps(Point pos) {
        if (mapPath.containsKey(pos)){
            return mapPath.get(pos).size();
        }else {
            Util.printWarn("getPathFps:"+this+pos);
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
        Main.totalSellValue += good.value;
        Main.totalSellSize ++;
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

    public boolean canCarryGood(Good good) {
        // 计算能否去取该货物，默认机器人在泊口
        int dis = getPathFps(good.pos);
        // todo 到时候避让得根据所剩余时间计算  可调参
        if (dis <= good.leftFps() - 3 && good.isNotBook()){
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

    public boolean canSendToMe(Point pos) {
        if (sizeNotEnough()){
            return false;
        }
        // 是否可以将这个物品送到我这儿来，
        int dis = getPathFps(pos);
        if (deadLine == Const.totalFrame ){
            return true;
        }
        // 弹性参数可调 ，参数表示是否早点离开这个泊口
        return deadLine > Const.frameId + dis + 5;
    }

    public boolean notFinalShip() {
        // 不是轮船最后运输的泊口
        return deadLine < Const.totalFrame;
    }

    public boolean sizeNotEnough() {
        // 空间够
        return existGoods.size() + bookGoodSize >= this.capacity;
    }
}

