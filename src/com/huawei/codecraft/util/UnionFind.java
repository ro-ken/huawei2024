package com.huawei.codecraft.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * ClassName: RegionManager
 * Package: com.huawei.codecraft.way
 * Description: 为berth进行聚类的时候，根据并查集，使集合唯一
 */
public class UnionFind {
    public final Map<Point, Point> parent;

    public UnionFind(Set<Point> points) {
        parent = new HashMap<>();
        for (Point point : points) {
            parent.put(point, point);
        }
    }

    public Point find(Point point) {
        Point p = point;
        while (!p.equals(parent.get(p))) {
            p = parent.get(p);
        }
        // Path compression
        Point root = p;
        p = point;
        while (!p.equals(root)) {
            Point next = parent.get(p);
            parent.put(p, root);
            p = next;
        }
        return root;
    }

    public void union(Point point1, Point point2) {
        Point root1 = find(point1);
        Point root2 = find(point2);
        if (!root1.equals(root2)) {
            parent.put(root1, root2);
        }
    }
}

