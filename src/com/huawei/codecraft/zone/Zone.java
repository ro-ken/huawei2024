package com.huawei.codecraft.zone;

import com.huawei.codecraft.Util;
import com.huawei.codecraft.core.Berth;
import com.huawei.codecraft.core.Good;
import com.huawei.codecraft.core.Robot;
import com.huawei.codecraft.util.Point;
import com.huawei.codecraft.util.RegionValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * 可以联通的大区域
 */
public class Zone {
    public int id;  // 区域号
    public final Set<Robot> robots = new HashSet<>();       // 区域内机0器人
    public final Set<Berth> berths = new HashSet<>();       // 区域内泊口
    public final Set<Point> accessPoints = new HashSet<>();     // 可达点
    public final Set<Region> regions =  new HashSet<>();    // 区域

    public Zone(int id) {
        this.id = id;
    }

    public void addRegion(Region region) {
        this.regions.add(region);
        region.setZone(this);
    }

    @Override
    public String toString() {
        return "Zone{" +
                "id=" + id +
                ", robots=" + robots +
                ", berths=" + berths +
                ", regions=" + regions +
                '}';
    }

    /**
     * 机器人到达了泊口，看是否要重新安排该机器人
     *
     * @param robot
     * @return 重新分返回true，否则返回false
     */
    public boolean reAssignRobot(Robot robot) {
        Region src = robot.region;
        // 当区域只有一个机器人，区域还有高价值货，就不调度
        if (src.assignedRobots.size() == 1){
//            if (src.haveHigherValueGoodThanExp(1)){
//                return false;
//            }
            return false;       // 只有一个机器人默认不调度到其他区域
        }
        // 全局考虑是否重新分配该机器人，
        // 机器人1个、2个；所有区域繁忙情况，繁忙区域是否应该调度本区域的机器人，还是调度临近区域
        // 只计算robot是否应该从该区域分出去
        // 比较分出去的收益 > 待在原地的收益，计算分到哪个区域下面最大收益
        Util.printDebug(robot+"开始计算换区");
        double maxProf = 0;   // 计算最大收益
        Region tar = null;  // 如果要分出去的目标区域
        for (Region region : src.neighborRegions) {
            double profit = calcChangeRobotProfit(src,region);
            if (profit > maxProf){
                maxProf = profit;
                tar = region;
            }
        }
        if (tar != null){
            // 交换该机器人
            changeRegionRobot(src,tar,robot);
            Util.printDebug("reAssignRobot:交换成功！"+robot+src+tar);
            return true;
        }else {
            Util.printDebug("reAssignRobot:失败！"+robot+src+tar);
            return false;
        }
    }

    private double calcChangeRobotProfit(Region src, Region tar) {
        // 计算将src的机器人分配到tar区域后产生的收益
        double srcV = src.removeRobotLoss();
        double destV = tar.addRobotProfit();
        // 计算去一个周期的代价与收益，加上路上的时间
        double dist = src.calcToRegionDis(tar);
        double profit = (Good.maxSurvive - 2*dist) * destV;
        double loss = Good.maxSurvive * srcV;
//        Util.printDebug("profit:"+profit+"loss:"+loss);
        return profit - loss;
    }

    private void changeRegionRobot(Region src, Region tar, Robot robot) {
        // 将机器人从src 交换给 tar;
        if (!src.assignedRobots.contains(robot)){
            Util.printErr("changeRegionRobot:交换失败");
            return;
        }
        src.assignedRobots.remove(robot);
        tar.assignRobots(robot);
    }

    // 首次根据静态信息分配机器人数量
    public void assignRegionRobotNum() {
        // 给每个区域分配机器人数量
        // 取出每个联通区有那些区域以及机器人
        // 先计算每个区域该分多少机器人
        int region_num = regions.size();
        int robot_num = robots.size();
        if (region_num > robot_num){
            ArrayList<Region> regs = new ArrayList<>(regions);
            // 区域数多，每次选出价值最大的
            while (robot_num > 0){
                Region tar = regs.get(0);
                RegionValue tarV = tar.staticValue.get(1);
                for (Region reg : regs) {
                    // 先判断区域是否都充足
                    RegionValue regV = reg.staticValue.get(1);
                    if (regV.isAreaRich() && regV.getExpStep() < tarV.getExpStep()){
                        tar = reg;
                        // 都充裕比谁距离小
                    }else {
                        // 都不充裕比谁面积大
                        if (!tarV.isAreaRich() && reg.accessiblePoints.size() > tar.accessiblePoints.size()){
                            tar = reg;
                        }
                    }
                }
                tar.staticAssignNum = 1;
                robot_num --;
                regs.remove(tar);
            }
        }else {
            ArrayList<Region> regs = new ArrayList<>(regions);
            // 机器人更多
            for (Region reg : regs) {
                reg.staticAssignNum = 1;    // 每个泊口先分一个
            }
            robot_num -= region_num;
            while (robot_num > 0){
                regs = new ArrayList<>(regions);
                for (int i = 0; i < region_num; i++) {
                    if (robot_num == 0) break;  // 分完了
                    // 直到分完所有机器人
                    Region tar = regs.get(0);
                    RegionValue tarV = tar.staticValue.get(2);
                    for (Region reg : regs) {
                        // 先判断区域是否都充足
                        RegionValue regV = reg.staticValue.get(2);
                        if (regV.isAreaRich() && regV.getExpStep() < tarV.getExpStep()){
                            tar = reg;
                            tarV = regV;
                            // 都充裕比谁距离小
                        }else {
                            // 都不充裕比谁面积大
                            if (!tarV.isAreaRich() && reg.accessiblePoints.size() > tar.accessiblePoints.size()){
                                tar = reg;
                            }
                        }
                    }
                    tar.staticAssignNum += 1;
                    robot_num --;
                    regs.remove(tar);
                }
            }
        }
    }

    // 首次根据静态信息分配机器人
    public void assignSpecificRegionRobot() {
        ArrayList<Robot> rs = new ArrayList<>(this.robots);
        for (Region region : this.regions) {
            for (int i = 0; i < region.staticAssignNum; i++) {
                Robot tar =rs.get(0);
                int min = region.getClosestBerthPathFps(tar.pos);
                for (int j = 1; j < rs.size(); j++) {
                    int t = region.getClosestBerthPathFps(rs.get(j).pos);
                    if (t < min){
                        min = t;
                        tar = rs.get(j);
                    }
                }
                region.assignRobots(tar); // 将该机器人分配好
                rs.remove(tar);
            }
        }
        if (!rs.isEmpty()){
            Util.printErr("assignSpecificRegionRobot:有机器人未被分配！");
        }
    }
}
