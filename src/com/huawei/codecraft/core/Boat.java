package com.huawei.codecraft.core;

import com.huawei.codecraft.Const;
import com.huawei.codecraft.Util;
import com.huawei.codecraft.util.BoatLastTask;
import com.huawei.codecraft.util.BoatStatus;
import com.huawei.codecraft.util.Twins;
import com.huawei.codecraft.zone.Region;

import java.util.*;

import static com.huawei.codecraft.Const.*;

// 轮船
public class Boat {
    public int id;

    // 目标泊位，如果目标泊位是虚拟点，则为-1
    public int berthId;

    // 状态：0移动；1正常运行（(即装货状态或运输完成状态）；2表示泊位外等待状态
    public int readsts;
    BoatStatus status=BoatStatus.FREE;
    public static int capacity;
//    public
    public Berth bookBerth;
    public int startFrame;
    public int goodSize;
    BoatLastTask task ;

    public Boat(int id) {
        this.id = id;
        task = new BoatLastTask(this);
    }



    public void schedule() {
        if (task.lastPeriod()){
            // 最后周期调度
            lastPeriodSched();
        }else {
            // 常规调度
            normalSched();
        }
    }

    private void lastPeriodSched() {
        if (status == BoatStatus.SHIP){
            // 行驶状态
            if (isArrive()){
                changeLoadMode();
            }
        }
        if (status == BoatStatus.LOAD){
            if (mustGotoVirtual()){
                Util.printDebug("船最后一次调度：");
                clacGoods();//结算货物
                goToVirtual();
            }else {
                if (isLoadFinish()){
                    if (task.isLastBerth()){
                        return;     // 最后一个泊口，等最后一帧在走
                    }
                    // todo 判断是否等一会或直接走
                    clacGoods();//结算货物
                    Berth berth = task.getNextBerth();  // 换下一个泊口
                    changeBerthAndShip(berth);
                }
            }
        }
    }

    private boolean mustGotoVirtual() {
        // 时间不够，必须回虚拟点了,todo 这里可以试一下能不能卖掉
        if (frameId + bookBerth.transport_time >= totalFrame){
            return true;
        }
        return false;
    }


    private void normalSched() {
        if (status == BoatStatus.FREE){
            // 没有任务
            findBerthAndShip();
        }
        if (status == BoatStatus.SHIP){
            // 行驶状态
            if (isArrive()){
                Util.printLog(this+"boat arrive："+bookBerth);
                changeLoadMode();
            }
        }
        if (status == BoatStatus.LOAD){
            // 时间不够，需要返航进入周期
            if (task.isBerthNeedBack()){
                clacGoods();//结算货物
                goToVirtual();
            }else {
                if (isLoadFinish()){
                    clacGoods();//结算货物
                    loadFinishNextStep();
                }
            }
        }
        if(status == BoatStatus.GO){
            if (isArrive()){
                resetBoat();        // 重置船
                // 需要判断是否进入最后周期
                if (task.canIntoLastPeriod()){
                    doLastPeriod();
                }else {
                    findBerthAndShip();
                }
            }
        }
    }

    private void doLastPeriod() {
        // 最后一个周期任务
        task.changeLastPeriodMode();
        Berth berth = task.getNextBerth();
        changeBerthAndShip(berth);
        setDeadLine(berth);
    }

    private void setDeadLine(Berth berth) {
        // 给这个泊口设定deadLine
        berth.setDeadLine(frameId + berth.transport_time + BoatLastTask.lastSecondStayTime);
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
            if (task.canBerthgotoBerth(next)){
                changeBerthAndShip(next);
                return;
            }
        }
        // 容量不够了，或没时间，先回去
        goToVirtual();
    }

    private void changeBerthAndShip(Berth next) {
        resetBookBerth();
        if (next == null)
            return;
        shipToBerth(next);
    }

    private void changeLoadMode() {
        // 到泊口了，变为装货状态
        status = BoatStatus.LOAD;
        // 更新装货时间
        startFrame = Const.frameId;
    }

    public static void init() {
        // 为每个泊船分配泊位
        List<Berth> berthList =  new ArrayList<>(Arrays.asList(berths));// 剩余未分配泊口
        ArrayList<Berth> tmp;
        int boatId = 0;
        for (Region region : regionManager.regions) {
            tmp = region.getCloestTwinsBerth(); // 找出区域内的成对泊口为一组，分配给一艘船
            while (tmp.size() > 1){
                // 将泊位
                List<Berth> addlist = new ArrayList<>(tmp.subList(0, 2));
                boats[boatId++].task.addBoath(addlist); // 每艘船分配2个泊口
                tmp.removeAll(addlist);
                berthList.removeAll(addlist);
            }
        }
        // 剩余的泊口按照价值排序，todo 这里先不算，直接分配以后在算
        while (boatId<boat_num){
            boats[boatId++].task.addBoath(berthList.subList(0,2));  // 如果不是1v2，这里要修改
            berthList.remove(0);
            berthList.remove(0);
        }
//        Util.printDebug("打印boats 分配信息");
//        for (Boat boat : boats) {
//            Util.printDebug(boat.id + ":"+boat.task.berths);
//        }
    }



    private boolean isArrive() {
        return readsts == 1;
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
        Util.printShip(id,bookBerth.id);
    }

    private void goToVirtual() {
        resetBookBerth();
        readsts = 0;
        status = BoatStatus.GO;
        Util.printGo(id);
    }

    private void clacGoods() {
        int left = capacity - goodSize;
        int loadGoods = Math.min(countGoods(),bookBerth.existGoods.size()); // 容量无限下这段时间装载量
        int realLoad = Math.min(left,loadGoods);    // 实际装载量

        Util.printLog("船的装载："+goodSize+"/"+capacity+"，单次装载量："+realLoad + "，泊口货物："+bookBerth.existGoods.size()+"，装载时间："+(Const.frameId - startFrame - 1));
        // 互相清算货物
        goodSize += realLoad;
        bookBerth.removeGoods(realLoad);
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
                ", berthId=" + berthId +
                ", status=" + readsts +
                '}';
    }

    private Berth pickTaskBerth() {
        // 选择task里的泊位,去货物多的
        if (task.berths.size() != 2){
            Util.printErr("pickTaskBerth");
            return null;
        }

        // 选择task中价值最高的泊位
        Berth target = task.berths.get(0);
        int maxVal = target.existValue;

        for (Berth berth : task.berths) {
            if (berth.bookBoats.size()>0){
                continue;
            }
            if (berth.existValue>maxVal){
                maxVal = berth.existValue;
                target = berth;
            }
        }
        return target;
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