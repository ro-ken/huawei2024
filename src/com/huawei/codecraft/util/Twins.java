package com.huawei.codecraft.util;

import java.util.Objects;

// 保存两个相同类型元素的容器
public class Twins <T1,T2>{
    private T1 obj1;
    private T2 obj2;

    public Twins(T1 obj1, T2 obj2) {
        this.obj1 = obj1;
        this.obj2 = obj2;
    }

    @Override
    public String toString() {
        return "Twins{" +
                "obj1=" + obj1 +
                "\nobj2=" + obj2 +
                '}';
    }

    public T1 getObj1() {
        return obj1;
    }

    public T2 getObj2() {
        return obj2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Twins)) return false;
        Twins<?, ?> twins = (Twins<?, ?>) o;
        return Objects.equals(obj1, twins.obj1) && Objects.equals(obj2, twins.obj2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(obj1, obj2);
    }

    public boolean contains(Object robot) {
        return obj1.equals(robot) || obj2.equals(robot);
    }
}
