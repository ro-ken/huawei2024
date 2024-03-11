package com.huawei.codecraft.way;

import com.huawei.codecraft.core.Berth;
import com.huawei.codecraft.core.Robot;
import com.huawei.codecraft.util.Point;

import java.util.HashSet;
import java.util.Set;

/**
 * ClassName: Region
 * Package: com.huawei.codecraft.way
 * Description: 划分地图为不同的区域，方便搜索和寻路
 */
public class Region {
    private final int id;
    private final Set<Berth> berths;
    private final Set<Point> accessiblePoints;
    private final Set<Robot> assignedRobots;

    public Region(int id) {
        this.id = id;
        this.berths = new HashSet<>();
        this.accessiblePoints = new HashSet<>();
        this.assignedRobots = new HashSet<>();
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
}
