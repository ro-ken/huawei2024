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

    public ArrayList<Berth> berths = new ArrayList<>();    // 最后周期的负责的泊口，最后周期按照顺序调度，berth[0],berth[1],...

    private int T0 = 0; // 静态周期
    private int T = 0; // 完整周期
    private int flexTime = 20; // 弹性时间

    private int latestT=0;  // 最迟进入周期时间

    public static int lastSecondStayTime = 5;   // 预计在倒数第二个船停留的时间 ，todo 后面可以动态算期望


    public BoatLastTask(Boat boat) {
        this.boat = boat;
    }

    public void addBoath(List<Berth> berths) {
        this.berths.addAll(berths);  // 未排序
        Util.printLog(boat+":"+berths);
        updateTime();
    }

    private void updateTime() {
        // 更新周期时间
        T0 = b2bFps + berths.get(0).transport_time + berths.get(1).transport_time;
        // 里面应该传入T，todo 暂时+10替代一下
        T = T0 + berths.get(0).expectLoadTime(T0) + berths.get(1).expectLoadTime(T0) + 10;

        latestT = totalFrame - T - flexTime;
    }


    public boolean lastPeriod() {
        return status == 1;
    }

    public boolean isBerthNeedBack() {
        // 在泊口装货的的时候，需要返回
        if (latestT == 0){
            return false;
        }
        if (frameId>=latestT-boat.bookBerth.transport_time){
            return true;
        }
        return false;
    }

    public boolean canIntoLastPeriod() {
        // 去一个泊口装货时间不够往返，进入，否则不进
        int b2x = Math.max(berths.get(0).transport_time,berths.get(1).transport_time);
        if (frameId >= latestT - b2x * 2 - 5){
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
        // todo
        int costTime = nextBerth.transport_time + b2bFps + 5;// 装货预留时间
        if (frameId >= latestT - costTime){
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
}
