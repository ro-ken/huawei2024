package com.huawei.codecraft.zone;

import com.huawei.codecraft.core.Berth;
import com.huawei.codecraft.core.Robot;
import com.huawei.codecraft.util.Point;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * ClassName: Region
 * Package: com.huawei.codecraft.way
 * Description: 划分地图为不同的区域，方便搜索和寻路
 */
public class Region {
    public final int id;                           // 区域 id
    public final Set<Berth> berths;                // 区域中的泊位
    public final Set<Point> accessiblePoints;      // 区域中的可达到点
    public final Set<Robot> assignedRobots;        // 分配给区域的机器人
    public final ArrayList<Region> neighborRegions;     // 距离当前region最近的region

    /**
     * 构造函数
     *
     */
    public Region(int id) {
        this.id = id;
        this.berths = new HashSet<>();
        this.accessiblePoints = new HashSet<>();
        this.assignedRobots = new HashSet<>();
        this.neighborRegions = new ArrayList<>();
    }

    public void addBerth(Berth berth) {
        this.berths.add(berth);
    }

    public void addAccessiblePoint(Point point) {
        this.accessiblePoints.add(point);
    }

    public void assignRobot(Robot robot) {
        this.assignedRobots.add(robot);
    }

    public int getId() {
        return id;
    }

    public Set<Berth> getBerths() {
        return berths;
    }

    public Set<Point> getAccessiblePoints() {
        return accessiblePoints;
    }

    public Set<Robot> getAssignedRobots() {
        return assignedRobots;
    }
    public ArrayList<Region> getNeighborRegions() {
        return neighborRegions;
    }
    @Override
    public String toString() {
        return "Region{" +
                "id=" + id +
                ", berths=" + berths +
                ", assignedRobots=" + assignedRobots +
                '}';
    }

    // 获取该区域内两个最近的泊口,次近的，...
    public ArrayList<Berth> getCloestTwinsBerth() {
        if (berths.size()<2){
            return new ArrayList<>();
        }
        if (berths.size() == 2){
            return new ArrayList<>(berths);
        }
        ArrayList<Berth> clone = new ArrayList<>(berths);
        ArrayList<Berth> res = new ArrayList<>();
        do {
            ArrayList<Berth> tmp = new ArrayList<>(clone);
            Berth b0 = clone.get(0);
            Berth b1 = clone.get(1);
            int min = b0.getPathFps(b1.pos);
            for (int i = 0; i < tmp.size()-1; i++) {
                for (int j = i+1; j < tmp.size(); j++) {
                    if (tmp.get(i).getPathFps(tmp.get(j).pos) < min){
                        min = tmp.get(i).getPathFps(tmp.get(j).pos);
                        b0 = tmp.get(i) ; b1 = tmp.get(j);
                    }
                }
            }
            res.add(b0);res.add(b1);
            clone.remove(b0);clone.remove(b1);
        }while (clone.size()>=2);

        return res;
    }
}
