package com.huawei.codecraft.core;

import com.huawei.codecraft.Const;
import com.huawei.codecraft.Util;
import com.huawei.codecraft.util.BoatStatus;

// 轮船
public class Boat {
    public int id;

    // 目标泊位，如果目标泊位是虚拟点，则为-1
    public int berthId;

    // 状态：0移动；1正常运行（(即装货状态或运输完成状态）；2表示泊位外等待状态
    public int readsts;
    BoatStatus status=BoatStatus.FREE;
//    public
    public Berth bookBerth;
    public int startFrame;
    public int goodSize;

    public Boat(int id) {
        this.id = id;
    }

    public void schedule() {
        if (status == BoatStatus.FREE){
            // 没有任务
            ShipToBerth();
        }
        if (status == BoatStatus.SHIP){
            // 行驶状态
            if (isArrive()){
                waitLoad();
            }else {
                refreshTime();
            }
        }
        if (status == BoatStatus.LOAD){
            Util.printLog("驶入，装货.....");
            if (loadFinish()){
                clacGoods();//结算货物
                goToVirtual();
            }
        }
        if(status == BoatStatus.GO){
            if (isArrive()){
                // 到达，重新分配任务
                ShipToBerth();
            }
        }
    }

    private void waitLoad() {
        // 等待装货
        status = BoatStatus.LOAD;
    }

    private boolean isArrive() {
        return readsts == 1;
    }

    private void ShipToBerth() {
        bookBerth = pickBerth();
        bookBerth.addBoat(this);
        readsts = 0; // 状态转换成移动
        status = BoatStatus.SHIP;
        Util.printShip(id,bookBerth.id);
    }

    private void doTask() {
        // 走了泊位置位null;

    }

    private void goToVirtual() {
        Util.printLog("hello");
        bookBerth.bookBoats.remove(this);
        bookBerth = null;
        readsts = 0;
        status = BoatStatus.GO;
        Util.printGo(id);
    }

    private void clacGoods() {
        int left = Const.boat_capacity - goodSize;
        int loadGoods = Math.min(countGoods(),bookBerth.existGoods.size()); // 容量无限下这段时间装载量
        int realLoad = Math.min(left,loadGoods);    // 实际装载量

        // 互相清算货物
        goodSize += realLoad;
        bookBerth.removeGoods(realLoad);
    }

    private boolean loadFinish() {
        int count = countGoods();
        // 没货了或装不下
        if (count >= bookBerth.existGoods.size() || count + goodSize >=Const.boat_capacity){
            // 装完了
            return true;
        }
        else return false;
    }

    // 计算已经装了多少货物
    private int countGoods() {
        int fps = Const.frameId - startFrame;
        int count = fps * bookBerth.loading_speed;
        return count;
    }

    private void refreshTime() {
        startFrame = Const.frameId;
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
                ", berthId=" + berthId +
                ", status=" + readsts +
                '}';
    }

    // 选择泊位
    private Berth pickBerth() {
        // 尽量选没有的
        int maxVal = -1;
        Berth target = null;

        for (Berth berth : Const.berths) {
            if (berth.bookBoats.size()>0){
                continue;
            }
            if (berth.existValue>maxVal){
                maxVal = berth.existValue;
                target = berth;
            }
        }
        if (target == null){
            for (Berth berth : Const.berths) {
                if (berth.bookBoats.size()>1){
                    continue;
                }
                if (berth.existValue>maxVal){
                    maxVal = berth.existValue;
                    target = berth;
                }
            }
        }
        if (target == null){
            for (Berth berth : Const.berths) {
                if (berth.existValue>maxVal){
                    maxVal = berth.existValue;
                    target = berth;
                }
            }
        }
        return target;
    }
}