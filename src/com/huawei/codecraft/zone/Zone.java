package com.huawei.codecraft.zone;

import com.huawei.codecraft.core.Berth;
import com.huawei.codecraft.core.Robot;
import com.huawei.codecraft.util.Point;

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
}
