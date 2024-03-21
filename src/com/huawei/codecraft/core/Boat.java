package com.huawei.codecraft.core;

import com.huawei.codecraft.Const;
import com.huawei.codecraft.Util;
import com.huawei.codecraft.util.BoatLastTask;
import com.huawei.codecraft.util.BoatStatus;
import com.huawei.codecraft.util.Twins;
import com.huawei.codecraft.zone.Region;
import com.huawei.codecraft.zone.RegionManager;

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


    public void schedule2() {
        if (task.lastPeriod()){
            // 最后周期调度
            lastPeriodSched();
        }else {
            // 常规调度
            normalSched();
        }
    }

    public void schedule() {
        if (task.lastPeriod()){
            // 最后周期调度
            lastPeriodSched();
        }else {
            // 常规调度
            normalSched1();
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
        goToVirtual();

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

    private void normalSched1() {
        if (status == BoatStatus.FREE){
            // 没有任务
            ShipNextBerth();
        }
        if (status == BoatStatus.SHIP){
            // 行驶状态
            if (isArrive()){
                Util.printLog(this+"boat arrive："+bookBerth);
                changeLoadMode();
            }
        }
        if (status == BoatStatus.LOAD){
            handleNormalLoadMode();
        }
        if(status == BoatStatus.GO){
            if (isArrive()){
                resetBoat();        // 重置船
                // 需要判断是否进入最后周期
                if (task.canIntoLastPeriod1()){
                    doLastPeriod();
                }else {
                    ShipNextBerth();
                }
            }
        }
    }

    private void handleNormalLoadMode() {
        if (boatIsFull()){
            Util.printLog("货船已满");
            clacGoods();//结算货物
            goToVirtual();
            return;
        }
        if (isLoadFinish()){
            if (bookBerth == task.getFirstBerth()){
                if (needGoToSecondBerth()){
                    Util.printLog(this+"驶入berth[1]，当前"+bookBerth);
                    clacGoods();//结算货物
                    ShipNextBerth();
                }
            }else {
                // 在第二个泊口
                if (needGoToVirtual()){
                    clacGoods();//结算货物
                    goToVirtual();
                }
            }
        }
    }

    private boolean needGoToVirtual() {
        // 在第二个泊口是否需要返回虚拟点
        // 且预估到第二个点的时间
        int end = task.getClosestNormalEndT();
        int needGoFid = end - bookBerth.transport_time;
        if (frameId > needGoFid){
            return true;
        }
        return false;
    }

    private boolean needGoToSecondBerth() {
        int realLoad = getRealLoad();
        int left = capacity - realLoad + task.getSecondBerth().existGoods.size();
        if (left < 6) return true;  // 货满了赶紧走

        // 且预估到第二个点的时间
        int end = task.getClosestNormalEndT();
        int needGoFid = end - task.getSecondBerth().transport_time - b2bFps - Math.min(50,capacity-realLoad);
        if (frameId > needGoFid){
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
//        assignBerth();
        assignBerthAvg();

        for (Boat boat : boats) {
            boat.task.sortBerth();
        }

        Util.printDebug("打印boats 分配信息");
        for (Boat boat : boats) {
            Util.printDebug(boat.id + ":");
            for (Berth berth : boat.task.berths) {
                Util.printLog(berth+":时间"+berth.transport_time+berth.staticValue);
            }
        }
    }


    private static void assignBerthAvg() {
        // 为每个泊船分配泊位，每艘船每次拉货平均分配：minT * （两个泊口期望平均产货）
        List<Berth> berthList =  new ArrayList<>(Arrays.asList(berths));// 剩余未分配泊口
        ArrayList<Berth> tmp;
        int boatId = 0;
        // 计算平均 货物
        while (berthList.size()>0){
            Twins<Berth,Berth> twins = getAvgCarrySpeedBerths(berthList);
            boats[boatId++].task.addBoath(twins); // 每艘船分配2个泊口
        }
    }

    private static Twins<Berth, Berth> getAvgCarrySpeedBerths(List<Berth> berthList) {

        if (berthList.size()<2){
            Util.printErr("getAvgCarrySpeedBerths");
            return null;
        }
        double max =0;
        double ms = 0;
        Berth tar1 = null;
        // 先选择最高产速
        for (Berth berth : berthList) {
            double t = berth.staticValue.get(1).getGoodNum()/Good.maxSurvive * berth.transport_time;
            if (t>max){
                max = t;
                tar1 = berth;
                ms = berth.staticValue.get(1).getGoodNum()/Good.maxSurvive;
            }
        }
        berthList.remove(tar1);
        Berth tar2 = berthList.get(0);
        double min = unreachableFps;
        for (Berth berth : berthList) {
            if (berth == tar2) continue;
            double ts = berth.staticValue.get(1).getGoodNum()/Good.maxSurvive;
            double fps = berth.transport_time + tar1.transport_time + b2bFps;
            double total = (ts+ms) * fps;   // 产量 = 产速 * 周期
            if (total < min){
                min = total;
                tar2 = berth;
            }
        }
        berthList.remove(tar2);
        return new Twins<>(tar1,tar2);
    }

    private static void assignBerth() {
        // 为每个泊船分配泊位
        List<Berth> berthList =  new ArrayList<>(Arrays.asList(berths));// 剩余未分配泊口
        ArrayList<Berth> tmp;
        int boatId = 0;
        // ① 同区域的为一组
        for (Region region : RegionManager.regions) {
            tmp = region.getClosestTwinsBerth(); // 找出区域内的成对泊口为一组，分配给一艘船
            while (tmp.size() > 1){
                // 将泊位
                List<Berth> addlist = new ArrayList<>(tmp.subList(0, 2));
                boats[boatId++].task.addBoath(addlist); // 每艘船分配2个泊口
                tmp.removeAll(addlist);
                berthList.removeAll(addlist);
            }
        }
        // ② 距离近的为一组
        while (!berthList.isEmpty()){
            tmp = getClosestTwinsBerth(berthList);
            if (tmp.size()>1){
                boats[boatId++].task.addBoath(tmp); // 每艘船分配2个泊口
            }else {
                break;
            }
        }

        // ③ 还没分完的是不同区域了，按价值高低，高 -> 低
        // 剩余的泊口按照价值排序，
        while (boatId<boat_num){
            tmp = getHighestLowestBerths(berthList);
            boats[boatId++].task.addBoath(tmp);  // 如果不是1v2，这里要修改
        }
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
        Util.printShip(id,bookBerth.id);
    }

    private void goToVirtual() {
        Util.printLog(this+"goto虚拟点");
        resetBookBerth();
        readsts = 0;
        status = BoatStatus.GO;
        Util.printGo(id);
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
        Util.printLog(this+":"+bookBerth+",结算货物：是first?"+(bookBerth == task.getFirstBerth()));
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

    // 选择泊位
    private Berth pickBerth() {
        // 尽量选没有的
        int maxVal = -1;
        Berth target = null;

        for (Berth berth : Const.berths) {
            if (!berth.bookBoats.isEmpty()){
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