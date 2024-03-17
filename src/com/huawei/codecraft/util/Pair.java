package com.huawei.codecraft.util;

// 目标价值类
public class Pair <T>implements Comparable{
    // 代比较的目标
    private T key;
    // object的价值，单位时间产生的价值，value = good.value / fps ;   降序排列
    private double value;

    public Pair(T obj, double value) {
        this.key = obj;
        this.value = value;
    }

    public T getKey() {
        return key;
    }

    public double getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "pair{" +
                "key=" + key +
                ", value=" + value +
                '}';
    }

    @Override
    public int compareTo(Object o) {
        Pair op = (Pair)o;
        return Double.compare(op.value,value);
    }
}
