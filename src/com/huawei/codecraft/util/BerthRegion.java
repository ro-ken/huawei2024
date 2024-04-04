package com.huawei.codecraft.util;

import com.huawei.codecraft.core.Berth;
import com.huawei.codecraft.core.Good;
import com.huawei.codecraft.core.Robot;

import java.util.PriorityQueue;

public class BerthRegion {
    // 对单个泊口按照价值进行分区
    Berth berth;    // 分配的泊口
    Robot robot;    // 分配的机器人
    int level;  // 泊口的区域级别，通过这个找到自己的队列，从1开始
    int workTime;   // 在该区域的工作时间，如果机器人只在此泊口工作，那么为 1000FPS
    PriorityQueue<Pair<Good>> domainGoodsByTime;    // 泊口价值大于本区域期望价值的物品，按照剩余时间排序
    int expMinValue;    // 期望最低价值
    int expMaxStep;     // 期望最远距离
    int expMaxStepNum;      // 期望最远距离所占的点数
    boolean rich ;  // 该区域是否充足
}
