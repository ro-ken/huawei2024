# %%
"""
分析货物生成情况
"""
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import seaborn as sns

# %%
# 输入数据
# 输入数据
map_name_list = ['1', '2', '3']
map_name = 'map' + map_name_list[0]  # 选择地图 0-->map1 1-->map2 2-->map3
input_map = '../maps/' + map_name + '.txt'  # map文件存放路径
input_goods = 'debug' + map_name[-1] + '.txt'  # 货物生成文件

with open(input_map) as file:
    content_map = file.readlines()

with open(input_goods) as file:
    content_goods = file.readlines()

# %%
# 背景坐标
map_base_x = []
map_base_y = []
# 空地 .
map_land_x = []
map_land_y = []
# 陆地主干道 >
map_land_main_x = []
map_land_main_y = []
# 海洋 *
map_sea_x = []
map_sea_y = []
# 海洋主航道 ~
map_sea_main_x = []
map_sea_main_y = []
# 障碍 #
map_wall_x = []
map_wall_y = []
# 机器人购买地块 R
map_lrobot_x = []
map_lrobot_y = []
# 船舶购买地块 S
map_lboat_x = []
map_lboat_y = []
# 泊位 B
map_berth_x = []
map_berth_y = []
# 靠泊区 K
map_berth_area_x = []
map_berth_area_y = []
# 海陆立体交通地块 C
map_C_x = []
map_C_y = []
# 海陆立体交通地块，同时为主干道和主航道 c
map_c_x = []
map_c_y = []
# 交货点 T
map_T_x = []
map_T_y = []

# 处理原始地图信息
for i in range(200):
    for j in range(200):
        # 背景点
        map_base_x.append(i)
        map_base_y.append(j)
        if content_map[i][j] == '.':  # 空地 .
            map_land_x.append(i)
            map_land_y.append(j)
        elif content_map[i][j] == '>':  # 陆地主干道 >
            map_land_main_x.append(i)
            map_land_main_y.append(j)
        elif content_map[i][j] == '*':  # 海洋 *
            map_sea_x.append(i)
            map_sea_y.append(j)
        elif content_map[i][j] == '~':  # 海洋主航道 ~
            map_sea_main_x.append(i)
            map_sea_main_y.append(j)
        elif content_map[i][j] == '#':  # 障碍 #
            map_wall_x.append(i)
            map_wall_y.append(j)
        elif content_map[i][j] == 'R':  # 机器人购买地块 R
            map_lrobot_x.append(i)
            map_lrobot_y.append(j)
        elif content_map[i][j] == 'S':  # 船舶购买地块 S
            map_lboat_x.append(i)
            map_lboat_y.append(j)
        elif content_map[i][j] == 'B':  # 泊位 B
            map_berth_x.append(i)
            map_berth_y.append(j)
        elif content_map[i][j] == 'K':  # 靠泊区 K
            map_berth_area_x.append(i)
            map_berth_area_y.append(j)
        elif content_map[i][j] == 'C':  # 海陆立体交通地块 C
            map_C_x.append(i)
            map_C_y.append(j)
        elif content_map[i][j] == 'c':  # 海陆立体交通地块，同时为主干道和主航道 c
            map_c_x.append(i)
            map_c_y.append(j)
        elif content_map[i][j] == 'T':  # 交货点 T
            map_T_x.append(i)
            map_T_y.append(j)
# %%
# 处理数据
goods_point_count = [[0] * 200 for _ in range(200)]
goods_point_value = [[0] * 200 for _ in range(200)]
goods_frame_count = {}  # 每帧出现的货物
goods_value_count = {}  # 每种价值出现的次数
frame_count = 0  # 某帧出现的货物
frame_id = 0  # 帧id
goods_count = 0  # 所有帧出现的货物总数
value_min = 99999  # 货品最小价值
value_max = 0  # 货品最大价值
value_sum = 0  # 货品总价值
value_high_count = 0  # 高价值货物总数
value_low_count = 0  # 低价值货物总数

for i in range(len(content_goods)):
    if content_goods[i].startswith('LOG::-------------frameId:'):  # 获取帧数据
        if frame_id != 0:
            goods_frame_count[frame_id] = frame_count
        frame_id = int(content_goods[i][:-15].split('Id:')[1])
        frame_count = 0
    elif content_goods[i].startswith('LOG::Good{pos'):  # 处理货物数据
        frame_count += 1
        goods_count += 1
        goods_point = content_goods[i][:-1][15:].split(')')[0]
        goods_x = int(goods_point.split(',')[0])
        goods_y = int(goods_point.split(',')[1])
        goods_value = int(content_goods[i][:-2].split('value=')[1])
        value_max = max(value_max, goods_value)
        value_min = min(value_min, goods_value)
        value_sum += goods_value
        # 统计每种价值出现的次数
        if goods_value not in goods_value_count:
            goods_value_count[goods_value] = 1
        else:
            goods_value_count[goods_value] += 1
        # 统计高低价值货物总数
        if goods_value > 100:
            value_high_count += 1
        else:
            value_low_count += 1
        goods_point_count[goods_x][goods_y] += 1
        goods_point_value[goods_x][goods_y] += goods_value

print('地图', map_name)
print('货物总数: ', goods_count)
print('货物平均价值: ', value_sum / goods_count)
print('货物总价值: ', value_sum)
print('value_max: ', value_max)
print('value_min: ', value_min)
print('高价值货物总数: ', value_high_count)
print('低价值货物总数: ', value_low_count)

scatter_data_count = np.array(goods_point_count)
scatter_data_value = np.array(goods_point_value)
mask_count = scatter_data_count == 0  # 掩码
mask_value = scatter_data_value == 0  # 掩码

# %%
# 控制网格参数
xmin = 0
xmax = 200.5
dx = 10
ymin = 0
ymax = 200.5
dy = 10

# %%
# 全局画图代码
plt.rcParams['font.family'] = ['sans-serif']
plt.rcParams['font.sans-serif'] = ['SimHei']

plt.figure(figsize=(25, 25))
plt.xlim((-0.5, 200.5))
plt.ylim((200.5, -0.5))

s1 = 40
s2 = 300
plt.scatter(map_base_y, map_base_x, s=s1, color='gainsboro')  # 背景
plt.scatter(map_land_y, map_land_x, s=s1, color='Gainsboro')  # 空地 .
plt.scatter(map_land_main_y, map_land_main_x, s=s1, color='NavajoWhite')  # 陆地主干道 >
plt.scatter(map_sea_y, map_sea_x, s=s1, color='SkyBlue', alpha=0.6)  # 海洋 *
plt.scatter(map_sea_main_y, map_sea_main_x, s=s1, color='DeepSkyBlue', alpha=0.6)  # 海洋主航道 ~
plt.scatter(map_wall_y, map_wall_x, s=s1, color='Black')  # 障碍 #
plt.scatter(map_berth_y, map_berth_x, s=s1, color='r')  # 泊位 B
plt.scatter(map_berth_area_y, map_berth_area_x, s=s1, color='MediumPurple')  # 靠泊区 K
plt.scatter(map_C_y, map_C_x, s=s1, color='Green', alpha=0.6)  # 海陆立体交通地块 C
plt.scatter(map_c_y, map_c_x, s=s1, color='LimeGreen', alpha=0.6)  # 海陆立体交通地块，同时为主干道和主航道 c
plt.scatter(map_lrobot_y, map_lrobot_x, s=s2, color='DarkOrange', marker='*')  # 机器人购买地块 R
plt.scatter(map_lboat_y, map_lboat_x, s=s2, color='Blue', marker='*')  # 船舶购买地块 S
plt.scatter(map_T_y, map_T_x, s=s2, color='DarkOrchid', marker='*')  # 交货点 T


# 绘制货物信息
heatmap = sns.heatmap(scatter_data_count, cmap='coolwarm', square=True, mask=mask_count, cbar_kws={'label': 'Values'})
# heatmap = sns.heatmap(scatter_data_value, cmap='coolwarm', square=True, mask=mask_value, cbar_kws={'label': 'Values'})

# plt.imshow(scatter_data_count, cmap='hot', interpolation='nearest')
# plt.colorbar()

plt.grid(True, color='k')
plt.xticks(np.arange(xmin, xmax, dx))  # x轴刻度
plt.yticks(np.arange(ymin, ymax, dy))  # y轴刻度
plt.tick_params(axis='x', labelsize=22)
plt.tick_params(axis='y', labelsize=22)
plt.tight_layout()  # 可选，自动调整布局
# plt.savefig('%s货品次数热图.png' % map_name)
# plt.savefig('%s货品价值热图.png' % map_name)
plt.show()

# %%
# 每种价值出现的次数导出到excel
# 指定列名
columns = ['value', 'times']
# 创建一个空的DataFrame
df = pd.DataFrame(columns=columns)

for key, value in goods_value_count.items():
    new_row = {'value': int(key), 'times': int(value)}
    df = df.append(new_row, ignore_index=True)

# df.to_excel('%s.xlsx' % map_name, index=False)

# %%
# 绘制每种价值出现的次数
plt.figure(figsize=(25, 25))

for key, value in goods_value_count.items():
    plt.scatter(int(key), int(value), color='r', s=s1)

plt.grid(True, color='k')
plt.tick_params(axis='x', labelsize=30)
plt.tick_params(axis='y', labelsize=30)
plt.xlabel("货品价值", family='SimHei', fontdict={'size': 40, 'weight': 'bold'})
plt.ylabel("出现次数", family='SimHei', fontdict={'size': 40, 'weight': 'bold'})
plt.title(map_name, family='SimHei', fontdict={'size': 40, 'weight': 'bold'})
plt.tight_layout()  # 可选，自动调整布局
# plt.savefig('%s货品价值统计.png' % map_name)
plt.show()
