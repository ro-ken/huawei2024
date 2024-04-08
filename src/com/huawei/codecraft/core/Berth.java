package com.huawei.codecraft.core;

import com.huawei.codecraft.Const;
import com.huawei.codecraft.Main;
import com.huawei.codecraft.Util;
import com.huawei.codecraft.util.*;
import com.huawei.codecraft.zone.Region;

import java.util.*;

import static com.huawei.codecraft.Const.*;
import static com.huawei.codecraft.zone.RegionManager.getPointProb;

// 泊位
public class Berth {
    public int id;
    public Point pos = new Point();
    public Point core = new Point();  // 泊位核心点
    public int transport_time;
    public int loading_speed;
    public Region region;  // 该泊口属于的区域，在区域初始化赋值
    public ArrayList<Berth> neighbors = new ArrayList<>();  // 该泊口的邻居，按照距离由近到远排序
    public PriorityQueue<Good> domainGoodsByValue = new PriorityQueue<>(new Comparator<Good>() {
        @Override
        public int compare(Good o1, Good o2) {
            // 需要被运输的货物,按照单位价值排序
            return Double.compare(o2.fpsValue,o1.fpsValue);
        }
    });
    public ArrayList<BerthArea> myAreas = new ArrayList<>();    // 本泊口的区域在细分给机器人
    public Deque<Good> domainGoodsByTime = new LinkedList<>();      // 需要被运输的货物,按照时间先后排序
    public Set<Boat> bookBoats = new HashSet<>();
    public HashSet<Point> boatInBerthArea = new HashSet<>();    // 靠泊区
    public List<HashSet<Point>> rectangleAreaPoints = new ArrayList<>();    // 泊位拥有的最大矩形区域
    public Deque<Good> existGoods = new LinkedList<>();     // 泊口存在的货物
    public int existValue=0;           // 泊口货物总价值
    public Map<Point,List<Point>> mapPath = new HashMap<>();   //  地图所有点到该泊位的路径信息
    public int deadLine = Const.totalFrame;     // 有效时间，超过这个时间轮船不装了，也不用往这里运了
    public final Map<Integer,Integer> pathLenToNumMap = new HashMap<>();      // 计算区域点到泊口长度对应个数的map
    public Map<Integer, RegionValue> staticValue = new HashMap<>();     // 区域静态价值
    public int points;      // berth拥有的最短路径点个数
    public int capacity = noLimitedSize;
    public int bookGoodSize;
    public int totalCarryValue = 0;
    public int totalCarrySize = 0;
    public int totalDis = 0;    // 取的货物总距离

    /************************************************分界线***********************************************/

    public static ArrayList<BerthArea> assignBerthToNewRobot() {
        // 寻找一片新区域给新增的机器人
        ArrayList<BerthArea> res = new ArrayList<>();
        BerthArea area = getBestSingleBerthArea();
        if (area == null) return res;

//        Util.printDebug("测试area与其他合并情况"+area);
//        Twins<BerthArea,BerthArea> tp = getCombineMoreValueArea(area);
//        Util.printBerthAreaTwins(tp);


        if (area.isRich()){
            res.add(area);
        }else {
            // 能否和邻居泊口进行合并，产生的价值更大
            Twins<BerthArea,BerthArea> tp = getCombineMoreValueArea(area);
            if (tp != null && tp.getObj1().getExpGoodNum() + tp.getObj2().getExpGoodNum() > area.getExpGoodNum()){
                res.add(tp.getObj1());
                res.add(tp.getObj2());
            }else {
                res.add(area);
            }
        }
        return res;
    }

    private static Twins<BerthArea,BerthArea> getCombineMoreValueArea(BerthArea area) {
        // 获取与该区域能合并的最大价值区域，目前计算距离最短的两个邻居
        Berth b1 = area.berth;
        double max = 0;
        Twins<BerthArea,BerthArea> res = null;
        for (int i = 0; i < b1.neighbors.size(); i++) {
            Berth b2 = b1.neighbors.get(i);
            Twins<BerthArea,BerthArea> tp = getTwinsBerthArea(b1,b2);

            if (tp != null){
                Util.printBerthAreaTwins(tp);
                if (res == null){
                    res = tp;
                }else {
                    double val = tp.getObj1().getExpGoodNum() + tp.getObj2().getExpGoodNum();
                    if (val > max){
                        max = val;
                        res = tp;
                    }
                }
            }
        }
        return res;
    }

    public static Twins<BerthArea, BerthArea> getTwinsBerthArea(Berth b1, Berth b2) {
        if (b1.noMoreArea() || b2.noMoreArea()){
            return null;    // 没有区域
        }

        // 算出这两区域的最佳区域组合
        double p = getPointProb()/totalFrame * Good.maxSurvive;     // Good.maxSurvive 周期内每个点产生的概率  ，计算出概率p = 0.0052;
        int total = Good.maxSurvive;   //往返fps，只有一半的时间是在去的路上
        double count = 0;
        BerthArea area1 = new BerthArea(b1,false);
        BerthArea area2 = new BerthArea(b2,false);
        Twins<Integer,Integer> tw1 = b1.getCurAreaMaxStepAndNum();
        Twins<Integer,Integer> tw2 = b2.getCurAreaMaxStepAndNum();
        int step1 = tw1.getObj1();
        int step2 = tw2.getObj1();
        int leftNum1 = tw1.getObj2();
        int leftNum2 = tw2.getObj2();
        double fps1=0,fps2=0;
        total -= b1.getPathFps(b2.pos) * 2;     // 去除两点的来回开销
        int index = Math.min(step1,step2);
        int maxStep1 = step1;
        int maxStep2 = step2;
        double goodNum1 = 0,goodNum2 = 0;
        while (count < total){
            boolean stop = true;
            if (index >=step1 && b1.pathLenToNumMap.containsKey(index)){
                stop = false;
                int num = b1.pathLenToNumMap.get(index) - leftNum1;
                leftNum1 = 0;
                double realNum= num * p;
                goodNum1 += realNum;
                double dis = index * realNum * 2;     //往返fps，只有一半的时间是在去的路上
                fps1 += dis;
                count += dis;
                maxStep1 = index;
            }
            if (index >=step2 && b2.pathLenToNumMap.containsKey(index)){
                stop = false;
                int num = b2.pathLenToNumMap.get(index) - leftNum2;
                leftNum2 = 0;
                double realNum= num * p;
                goodNum2 += realNum;
                double dis = index * realNum * 2;     //往返fps，只有一半的时间是在去的路上
                fps2 += dis;
                count += dis;
                maxStep2 = index;
            }
            index++;
            if (stop){
                break;
            }
        }
        if (count > total){
            fps1 *= total/count;
            fps2 *= total/count;
        }
        if (fps1 == 0 || fps2 == 0){
            return null;
        }
        area1.setCombineValue(goodNum1,maxStep1, (int) fps1);
        area2.setCombineValue(goodNum2,maxStep2, (int) fps2);
        return new Twins<>(area1,area2);
    }

    public static void init() {
        // 初始化邻居
        for (Berth berth : berths) {
            berth.initNeighbors();
        }
        // 根据机器人数量初始化区域
        if (Main.assignRobotNum <=0){
            // 未分配，手动计算分配数量
            clacAssignRobotNum();
        }
        Util.printLog("机器人预计购买数量为："+Main.assignRobotNum);
        assignRobotArea();
    }

    private static void assignRobotArea() {
        // 根据机器人数量分配机器人区域
        for (int i = 0; i < Main.assignRobotNum; i++) {
            // 首先分配到最大的区域
            BerthArea area = getBestSingleBerthArea();
            // 选择最近的购买点
            Point buyPos = area.berth.pickClosestRobotBuyPos();
            // ID 后面在修改
            Robot robot = new Robot(i,buyPos);
            robot.areas.add(area);
            area.enable(robot);
            preAssignRobot.add(robot);
        }
        // 根据首轮分配结果再次分配
        // 主要分配机器人area不够的区域，看能否调用其他区域的area
        ArrayList<Berth> berthList = new ArrayList<>(berths);
        while (berthList.size()>=2){
            Berth send = pickBiggestRedundantAreaBerth(berthList);
            if (send == null){
                break;
            }
            // 看邻居哪个机器人拿到这块地收益最大
            Berth take = null;
            double max = 0;
            Twins<BerthArea,BerthArea> maxTp = null;
//            Util.printLog("最大的区域为："+send);
            for (int i = 0; i < 2; i++) {
                Berth neighbor = send.neighbors.get(i);
                if (!berthList.contains(neighbor) || neighbor.myAreas.isEmpty() || !neighbor.myAreas.get(neighbor.myAreas.size()-1).single){
                    // 以上三种情况的区域不参与争夺
//                    Util.printLog("邻居："+neighbor+"不参与分配");
                    continue;
                }
                BerthArea neighborLastArea =  neighbor.myAreas.remove(neighbor.myAreas.size() - 1);
                Twins<BerthArea,BerthArea> tp = getTwinsBerthArea(send,neighbor);
//                Util.printLog("邻居："+neighbor+"tp："+tp);
                if (tp != null){
                    double expGood = tp.getObj1().getExpGoodNum() + tp.getObj2().getExpGoodNum();
                    Util.printLog("tp期望物品数："+expGood+"原area物品数："+neighborLastArea.getExpGoodNum());
                    if (expGood> neighborLastArea.getExpGoodNum() && expGood > max){
                        max = expGood;
                        maxTp = tp;
                        take = neighbor;
                    }
                }
                neighbor.myAreas.add(neighborLastArea); // 先加回去
            }
            if (maxTp != null){
                // 有其他区域要这块多于区域，重新分配机器人
                BerthArea tmp = take.myAreas.remove(take.myAreas.size() - 1);
                tmp.robot.areas.clear();
                ArrayList<BerthArea> berthAreas = new ArrayList<>();
                berthAreas.add(maxTp.getObj1());
                berthAreas.add(maxTp.getObj2());
                tmp.robot.setAreas(berthAreas); // 重新赋能
                // 此机器人不能在参与区域争夺
                Util.printLog("成功合并！"+berthAreas);
            }
            berthList.remove(send);
        }
    }

    private static Berth pickBiggestRedundantAreaBerth(ArrayList<Berth> berthList) {
        // 挑选最多冗余区域的泊口，没有则返回null
        // 即下一级level的GoodNum最大的
        Berth res = null;
        double goodNum = 0;
        for (Berth berth : berthList) {
            if (berth.staticValue.get(berth.myAreas.size()+1)!=null){
                double more = berth.staticValue.get(berth.myAreas.size()+1).getGoodNum() - berth.staticValue.get(berth.myAreas.size()).getGoodNum();
                if (more>goodNum){
                    goodNum = more;
                    res = berth;
                }
            }
        }
        return res;
    }

    private static void clacAssignRobotNum() {
        // 计算需要分配多少机器人
        Main.assignRobotNum= 0;
        for (Berth berth : berths) {
            double last = 0;
            for (Integer key : berth.staticValue.keySet()) {
                double curNum = berth.staticValue.get(key).getGoodNum();
                if (curNum-last>Main.minAddNumPerRobot){
                    Main.assignRobotNum ++;
                    last = curNum;
                }else {
                    break;
                }
            }
        }
    }

    public Point pickClosestRobotBuyPos() {
        // 找里berth最近的出生点
        Point tar = robotBuyPos.get(0);
        int min = unreachableFps;
        for (Point pos : robotBuyPos) {
            if (getPathFps(pos) < min){
                min = getPathFps(pos);
                tar = pos;
            }
        }
        return tar;
    }


    private boolean noMoreArea() {
        if (myAreas.isEmpty()){
            return false;
        }
        BerthArea area = myAreas.get(myAreas.size() - 1);
        // 最后一个区域都不满，则没更多区域了
        return !area.isRich();
    }

    private Twins<Integer, Integer> getCurAreaMaxStepAndNum() {
        // 获取当前已分配区域的最远距离和最远距离的个数
        if (myAreas.isEmpty()){
            return new Twins<>(0,0);
        }
        BerthArea end = myAreas.get(myAreas.size() - 1);
        return new Twins<>(end.getExpMaxStep(),end.getExpMaxStepNum());
    }

    private static BerthArea getBestSingleBerthArea() {
        // 获取所有区域剩余单一价值最大的区域
        BerthArea res = null;
        for (Berth berth : berths) {
            BerthArea area = berth.getLeftBestArea();
            if (area != null){
                if (res == null || (area.getExpGoodNum() > res.getExpGoodNum())){
                    res = area;
                }
            }
        }
        return res;
    }

    public BerthArea getLeftBestArea() {
        // 获取剩余区域的最大区域价值
        BerthArea area;
        if (myAreas.isEmpty()){
            area = new BerthArea(this,true);
        }else {
            BerthArea last = myAreas.get(myAreas.size()-1);
            if (!last.isRich()){
                return null;    // 最后一个区域都不够了
            }
            area = new BerthArea(this,true);
        }
        return area;
    }

    public void setDeadLine(int deadLine) {
        this.deadLine = deadLine;
    }

    public Berth(int id) {
        this.id = id;
    }

    public void assignRegion(Region region){
        this.region = region;
    }

    public int getPathFps(Point pos) {
        if (mapPath.containsKey(pos)){
            return mapPath.get(pos).size();
        }else {
            Util.printWarn("getPathFps:本泊口无此路径 "+this+pos);
            return Const.unreachableFps;
        }
    }

    @Override
    public String toString() {
        return "berth{" +
                "id=" + id +
                ", pos=" + pos +
                '}';
    }

    // 增加码头的货物
    public void addBerthGood(Good good){
        existGoods.add(good);
        existValue += good.value;
    }
    public int removeGood(){
        Good good = existGoods.pop();
        existValue -= good.value;
        Const.totalSellValue += good.value;
        Const.totalSellSize ++;
        if (existValue <0){
            existValue = 0;
            Util.printLog("ERROR! existValue <0");
        }
        return good.value;
    }

    // 移出货物
    public int removeGoods(int size) {
        int value = 0;
        for (int i = 0; i < size; i++) {
            value +=removeGood();
        }
        return value;
    }

    public void addBoat(Boat boat) {
        bookBoats.add(boat);
    }

    public boolean canCarryGood(Good good) {
        // 计算能否去取该货物，默认机器人在泊口
        int dis = getPathFps(good.pos);
        // todo 到时候避让得根据所剩余时间计算  可调参
        if (dis <= good.leftFps() - 2){
            // 加几帧弹性时间，怕绕路
            return true;
        }
        return false;
    }

    public void removeDomainGood(Good good) {
        // 清除管理区域的货物,统一删除region中的
        domainGoodsByTime.remove(good);
        domainGoodsByValue.remove(good);
        region.regionGoodsByTime.remove(good);
        region.regionGoodsByValue.remove(good);
        for (BerthArea myArea : myAreas) {
            myArea.areaGoodsByTime.remove(good);
        }
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

    public void initNeighbors() {
        // 初始化泊口邻居
        ArrayList<Berth> copy = new ArrayList<>(berths);
        copy.remove(this);
        while (!copy.isEmpty()){
            Berth tar = copy.get(0);
            int min = unreachableFps;
            for (Berth berth : copy) {
                int fps = getPathFps(berth.pos);
                if (fps < min){
                    min = fps;
                    tar = berth;
                }
            }
            neighbors.add(tar);
            copy.remove(tar);
        }
    }

    public int getAreaMaxStep() {
        BerthArea area = myAreas.get(myAreas.size() - 1);
        return area.getExpMaxStep();
    }

    public void setPos() {
        // 根据核心点设置代表点
        // 找周围8个点，靠近陆地的点为代表点
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                Point point = new Point(core.x+i,core.y+j);
                if (point.inBerthCenter()){
                    pos = point;
                }
            }
        }
    }

    public int getRobotToBerthMinFps() {
        int min = unreachableFps;
        // 所有机器人正常工作，判断最快一个机器人到达泊口的时间，
        for (BerthArea area : myAreas) {
            if (area.robot.noTask() || area.robot.bookBerth != this){
                // 不算已经在泊口的
                continue;
            }
            int fps;
            if (area.robot.isCarry()){
                fps = area.robot.route.leftPathLen();
            }else {
                fps = area.robot.route.leftPathLen() + getPathFps(area.robot.bookGood.pos);
            }
            if (fps<min){
                min = fps;
            }
        }
        return min;
    }
}

