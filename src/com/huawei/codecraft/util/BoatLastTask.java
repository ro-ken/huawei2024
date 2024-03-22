package com.huawei.codecraft.util;

import com.huawei.codecraft.Util;
import com.huawei.codecraft.core.Berth;
import com.huawei.codecraft.core.Boat;

import java.util.ArrayList;
import java.util.List;

import static com.huawei.codecraft.Const.*;

/**
 * 轮船的最后一个周期的任务
 */
public class BoatLastTask {
    Boat boat;
    private int status = 0; // 轮船是否进入最后周期

    private int endIndex = 0;  // 最后模式的下表
    // 暂定为1v2，若要改，很多都要改
    private int normalIndex = 0;  // 正常模式的下标

    public ArrayList<Berth> berths = new ArrayList<>();    // 最后周期的负责的泊口，最后周期按照顺序调度，berth[0],berth[1],...

    private int minT; // 最小完整周期，最后一个阶段的调度周期
    private int normalT;    // 前面正常周期  total = normalT * n + minT ;
    private int lastTFPS=0;  // 最迟进入周期时间

    public BoatLastTask(Boat boat) {
        this.boat = boat;
    }

    public void addBoath(List<Berth> berths) {
        this.berths.addAll(berths);  // 未排序
        Util.printLog(boat+":"+berths);
        updateTime();
    }

    public void addBoath(Twins<Berth, Berth> twins) {
        this.berths.add(twins.getObj1());
        this.berths.add(twins.getObj2());
        Util.printLog(boat+":"+berths);
        updateTime();
    }

    public int getMinT() {
        return minT;
    }

    private void updateTime() {
        // 更新周期时间
        int t = b2bFps + berths.get(0).transport_time + berths.get(1).transport_time;
        minT = t + Math.min(Boat.capacity,80);    // 确定最后一个周期时间，
        lastTFPS = totalFrame - minT;
        // 确定前面周期时间 Total = n*T1 + minT;
         clacNormalT(lastTFPS);
    }

    private void clacNormalT(int left) {
        for (int i = 1; i <= 10; i++) {
            int t = left / i;
            if (t >= minT){
                normalT = t;
            }else {
                break;
            }
        }
    }


    public boolean lastPeriod() {
        return status == 1;
    }

    public boolean isBerthNeedBack() {
        // 在泊口装货的的时候，需要返回
        if (lastTFPS == 0){
            return false;
        }
        if (frameId>=lastTFPS-boat.bookBerth.transport_time){
            return true;
        }
        return false;
    }

    public boolean canIntoLastPeriod() {
        // 去一个泊口装货时间不够往返，进入，否则不进
        int b2x = Math.max(berths.get(0).transport_time,berths.get(1).transport_time);
        if (frameId >= lastTFPS - b2x * 2 - 5){
            return true;
        }
        return false;
    }
    public boolean canIntoLastPeriod1() {
        // 去一个泊口装货时间不够往返，进入，否则不进
        int b2x = Math.min(berths.get(0).transport_time,berths.get(1).transport_time);
        if (frameId >= lastTFPS - b2x * 2 - 5){
            return true;
        }
        return false;
    }

    public Berth getNextBerth() {
        if (status == 0){
            // 正常模式
            if (boat.bookBerth == berths.get(0)){
                return berths.get(1);
            }else {
                return berths.get(0);
            }
        }else {
            // 最后模式
            if (endIndex<berths.size()){
                return berths.get(endIndex++);
            }else {
                Util.printErr("getNextBerth:end 模式访问出错！");
                return berths.get(0);
            }
        }
    }

    // 判断是否可以去下一个泊口，而不耽误最后的周期
    public boolean canBerthGotoBerth(Berth nextBerth) {

        int costTime = nextBerth.transport_time + b2bFps + 5;// 装货预留时间
        if (frameId >= lastTFPS - costTime){
            return false;
        }
        return true;
    }

    public void changeLastPeriodMode() {
        status = 1;
        endIndex = 0;
    }

    public boolean isLastBerth() {
        return endIndex == 2;
    }

    public void sortBerth() {
        // 将 berths 按照排序，调度顺序为 0号,1号,价值大的排后面
        if (this.berths.size() != 2){
            return;
        }
        // 最后应该是所有机器人往一点冲，要比较多机器人的价值
        if (this.berths.get(0).staticValue.get(2).getGoodNum() > this.berths.get(1).staticValue.get(2).getGoodNum()){
            Berth b0 = berths.remove(0);
            berths.add(b0);
        }
    }

    public Berth getFirstBerth() {
        return berths.get(0);
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

    public Berth getSecondBerth() {
        return berths.get(1);
    }

}
