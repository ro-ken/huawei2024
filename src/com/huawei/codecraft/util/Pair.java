package com.huawei.codecraft.util;

// 目标价值类
public class Pair <T>implements Comparable{
    // 代比较的目标
    private T object;
    // object的价值，单位价值花费的时间，从小到大排序  cost = fps / value;
    private double cost;

    public Pair(T obj, double cost) {
        this.object = obj;
        this.cost = cost;
    }

    public T getObject() {
        return object;
    }

    public double getCost() {
        return cost;
    }

    @Override
    public int compareTo(Object o) {
        Pair op = (Pair)o;
        return Double.compare(cost,op.cost);
    }
}
