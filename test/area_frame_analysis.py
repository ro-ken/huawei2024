# %%
"""
分析每个区域前n帧出现货物的情况
"""

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

# %%
# 输入数据
map_name = 'map1'
input_map = '../maps/' + map_name + '.txt'  # 地图文件
input_area = './point-v2/point' + map_name[3:] + '.txt'  # 分区文件
input_goods = '../goods/' + 'maps-3.6' + '.txt'  # 货物生成文件

with open(input_map) as file:
    content_map = file.readlines()

with open(input_area) as file:
    content_point = file.readlines()

with open(input_goods) as file:
    content_goods = file.readlines()

# %%
# 处理map数据
# 背景坐标
base_x = []
base_y = []
# 障碍物坐标 #
wall_x = []
wall_y = []
# 海洋坐标 *
sea_x = []
sea_y = []
# 陆地坐标 .
land_x = []
land_y = []
# 泊位坐标 B
berth_x = []
berth_y = []
# 机器人原始坐标 A
robot_x = []
robot_y = []

# 处理原始地图信息
for i in range(200):
    for j in range(200):
        # 背景点
        base_x.append(i)
        base_y.append(j)
        if content_map[i][j] == '#':
            wall_x.append(i)
            wall_y.append(j)
        elif content_map[i][j] == '*':
            sea_x.append(i)
            sea_y.append(j)
        elif content_map[i][j] == '.':
            land_x.append(i)
            land_y.append(j)
        elif content_map[i][j] == 'B':
            berth_x.append(i)
            berth_y.append(j)
        elif content_map[i][j] == 'A':
            robot_x.append(i)
            robot_y.append(j)

# %%
# 处理分区point数据
# 泊位坐标
berth_x_area = []  # 对应区域顺序
berth_y_area = []  # 对应区域顺序
# 陆地坐标
land_x_area = []  # 对应区域顺序
land_y_area = []  # 对应区域顺序
# 泊位临时坐标
berth_x_temp = []
berth_y_temp = []
# 陆地临时坐标
land_x_temp = []
land_y_temp = []

total_regions = int(content_point[0][:-1].split(': ')[1])  # 总区域
berth_id_list = {}  # 用于记录每个泊位对应的ID
point_id_list = {}  # 用于记录每个点对应的ID
region_id = -1

for i in range(len(content_point)):
    if content_point[i].startswith('Region ID:'):  # 开始处理该区域的数据
        region_id = content_point[i].split(': ')[1][:-1]  # 当前区域ID
        # 泊位坐标
        berth_x_temp = []
        berth_y_temp = []
        # 陆地坐标
        land_x_temp = []
        land_y_temp = []
    elif content_point[i].startswith('    Berth at: '):  # 泊位坐标
        berth_point = content_point[i][:-1].split(': ')[1]
        berth_id_list[berth_point] = region_id
        berth_x_temp.append(int(berth_point.split(',')[0][1:]))
        berth_y_temp.append(int(berth_point.split(',')[1][:-1]))
    elif content_point[i].startswith('    Accessible Points:'):  # 分区坐标
        land_point = content_point[i][:-2].split(': ')[1].split(' ')
        for each in land_point:
            land_x_temp.append(int(each.split(',')[0][1:]))
            land_y_temp.append(int(each.split(',')[1][:-1]))
            # 将该点直接映射到区域 方便下面处理货物信息
            point_id_list[each] = region_id
    elif content_point[i].startswith('  Assigned robots in region: '):  # 结束标识
        berth_x_area.append(berth_x_temp)
        berth_y_area.append(berth_y_temp)
        land_x_area.append(land_x_temp)
        land_y_area.append(land_y_temp)

# %%
# 处理货物goods数据
regions_frame = [3] * total_regions  # 用于计数 每个区域都取n个货物
frame_id = 0  # 帧id

# 指定列名
columns = ['regionID', 'frame', 'point', 'value']

# 创建一个空的DataFrame
df = pd.DataFrame(columns=columns)

df['regionID'] = df['regionID'].astype(int)
df['frame'] = df['frame'].astype(int)
df['value'] = df['value'].astype(int)

for i in range(len(content_goods)):
    if content_goods[i].startswith('frameId:'):  # 获取帧数据
        frame_id = int(content_goods[i].split(':')[-1][:-1])
    else:  # 处理货物数据
        goods_point = content_goods[i].split(' val:')[0]  # 坐标
        goods_value = int(content_goods[i].split(' val:')[1][:-1])  # 价值

        if goods_point in point_id_list:
            goods_region = int(point_id_list[goods_point])  # 区域
        else:
            continue
        # if regions_frame[goods_region] > 0:  # 该区域需要继续输出
        if frame_id <= 2000:  # 输出前n帧的所有信息
            new_row = {'regionID': goods_region, 'frame': frame_id, 'point': goods_point,
                       'value': goods_value}
            df = df.append(new_row, ignore_index=True)
            regions_frame[goods_region] -= 1

df.to_excel('%s-frame-2000.xlsx' % map_name, index=False)

# %%
# 全局画图代码
plt.rcParams['font.family'] = ['sans-serif']
plt.rcParams['font.sans-serif'] = ['SimHei']

plt.figure(figsize=(25, 25))
plt.xlim((-0.5, 200.5))
plt.ylim((200.5, -0.5))

s = 40

# 控制网格参数
xmin = 0
xmax = 200.5
dx = 10
ymin = 0
ymax = 200.5
dy = 10

plt.scatter(base_y, base_x, s=s, color='gainsboro')  # 背景
plt.scatter(wall_y, wall_x, s=s, color='k')  # 障碍物
# plt.scatter(sea_y, sea_x, s=s, color='skyblue')  # 海洋
plt.scatter(berth_y, berth_x, s=s, color='r')  # 港口坐标
# plt.scatter(robot_y, robot_x, s=s, color='blue')  # 机器人原始坐标


# 绘制分区
colors = ['lightcoral', 'orange', 'yellow', 'lawngreen', 'cyan', 'deepskyblue', 'blueviolet', 'pink', 'deeppink',
          'blue']
for i in range(total_regions):
    if i == 100:
        pass
    else:
        plt.scatter(berth_y_area[i], berth_x_area[i], s=350, alpha=1, marker='*', color=colors[i])
        plt.scatter(land_y_area[i], land_x_area[i], s=s, alpha=0.8, color=colors[i])

plt.grid(True, color='k')
plt.xticks(np.arange(xmin, xmax, dx))  # x轴刻度
plt.yticks(np.arange(ymin, ymax, dy))  # y轴刻度
plt.tick_params(axis='x', labelsize=22)
plt.tick_params(axis='y', labelsize=22)
plt.tight_layout()  # 可选，自动调整布局
# plt.savefig('%s_frame.png' % map_name)
plt.show()
