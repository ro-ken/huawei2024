#%%
"""
分析每个点离最近的港口的距离
并进行绘图和导出excel
"""
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

# %%
# 输入数据
map_name_list = ['1', '2', '3']
map_name = 'map' + map_name_list[0]
input_map = '../maps/' + map_name + '.txt'  # 地图文件
input_area = './point-v1/point2Region' + map_name[3:] + '.txt'  # 分区文件
input_berth2point = './berth2point/berth2point' + map_name[3:] + '.txt'  # 路径长度文件


with open(input_map) as file:
    content_map = file.readlines()

with open(input_area) as file:
    content_point = file.readlines()

with open(input_berth2point) as file:
    content_berth2point = file.readlines()

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

# 存入坐标信息
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
# 处理分区数据
# 泊位坐标
berth_x_area = []
berth_y_area = []
# 陆地坐标
land_x_area = []
land_y_area = []
# 泊位临时坐标
berth_x_temp = []
berth_y_temp = []
# 陆地临时坐标
land_x_temp = []
land_y_temp = []

total_regions = int(content_point[0][:-1].split(': ')[1])
berth_id_list = {}  # 用于记录每个泊位对应的ID
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
    if content_point[i].startswith('This region end'):  # 结束标识
        berth_x_area.append(berth_x_temp)
        berth_y_area.append(berth_y_temp)
        land_x_area.append(land_x_temp)
        land_y_area.append(land_y_temp)

# %%
# 处理路径长度数据
path_length = [[0] * 200 for _ in range(200)]  # 存储每个点的路径长度
path_berth = [[[0 for _ in range(2)] for _ in range(200)] for _ in range(200)]  # 存储每个点距离最近的泊位
count = 0

for i in range(len(content_berth2point)):
    if content_berth2point[i].startswith('Paths from Berths'):
        pass
    elif content_berth2point[i].startswith('Path from Berth '):
        points = content_berth2point[i][16:-2].split(' to ')  # [泊位坐标,点坐标]
        berth = points[0]
        point = points[1]
        point_x_single = int(point.split(',')[0][1:])
        point_y_single = int(point.split(',')[1][:-1])
        berth_x_single = int(berth.split(',')[0][1:])
        berth_y_single = int(berth.split(',')[1][:-1])
        length = len(content_berth2point[i + 1][:-2].split(' '))
        if length < path_length[point_x_single][point_y_single] or path_length[point_x_single][point_y_single] == 0:
            # print('(%s,%s) 更新' % (point_x, point_y))
            count += 1
            path_length[point_x_single][point_y_single] = length
            path_berth[point_x_single][point_y_single][0] = berth_x_single
            path_berth[point_x_single][point_y_single][1] = berth_y_single

# %%
# 全局画图代码
plt.rcParams['font.family'] = ['sans-serif']
plt.rcParams['font.sans-serif'] = ['SimHei']

plt.figure(figsize=(60, 60))
plt.xlim((-0.5, 200.5))
plt.ylim((200.5, -0.5))

# 控制网格参数
xmin = 0
xmax = 200.5
dx = 10
ymin = 0
ymax = 200.5
dy = 10

s1 = 40
s2 = 300
plt.scatter(map_base_y, map_base_x, s=s1, color='gainsboro')  # 背景
plt.scatter(map_land_y, map_land_x, s=s1, color='Gainsboro')  # 空地 .
# plt.scatter(map_land_main_y, map_land_main_x, s=s1, color='NavajoWhite')  # 陆地主干道 >
# plt.scatter(map_sea_y, map_sea_x, s=s1, color='SkyBlue')  # 海洋 *
# plt.scatter(map_sea_main_y, map_sea_main_x, s=s1, color='DeepSkyBlue')  # 海洋主航道 ~
plt.scatter(map_wall_y, map_wall_x, s=s1, color='Black')  # 障碍 #
plt.scatter(map_berth_y, map_berth_x, s=s1, color='r')  # 泊位 B
plt.scatter(map_berth_area_y, map_berth_area_x, s=s1, color='MediumPurple')  # 靠泊区 K
plt.scatter(map_C_y, map_C_x, s=s1, color='Green')  # 海陆立体交通地块 C
plt.scatter(map_c_y, map_c_x, s=s1, color='LimeGreen')  # 海陆立体交通地块，同时为主干道和主航道 c
plt.scatter(map_lrobot_y, map_lrobot_x, s=s2, color='DarkOrange', marker='*')  # 机器人购买地块 R
plt.scatter(map_lboat_y, map_lboat_x, s=s2, color='Blue', marker='*')  # 船舶购买地块 S
plt.scatter(map_T_y, map_T_x, s=s2, color='DarkOrchid', marker='*')  # 交货点 T



# 绘制分区
colors = ['lightcoral', 'orange', 'yellow', 'lawngreen', 'cyan', 'deepskyblue', 'blueviolet', 'pink', 'deeppink',
          'blue']
for i in range(total_regions):
    if i == 100:
        pass
    else:
        plt.scatter(berth_y_area[i], berth_x_area[i], s=350, alpha=1, marker='*', color=colors[i])
        plt.scatter(land_y_area[i], land_x_area[i], s=s1, alpha=0.8, color=colors[i])

for i in range(200):
    for j in range(200):
        if path_length[i][j] != 0:
            plt.text(j, i, str(path_length[i][j]), fontsize=11)

plt.grid(True, color='k')
plt.xticks(np.arange(xmin, xmax, dx))  # x轴刻度
plt.yticks(np.arange(ymin, ymax, dy))  # y轴刻度
plt.tick_params(axis='x', labelsize=40)
plt.tick_params(axis='y', labelsize=40)
plt.tight_layout()  # 可选，自动调整布局
plt.savefig('%s_length.png' % map_name)
# plt.show()

# %%
# 对路径长度进行归类输出
berth_length_times = [{} for _ in range(total_regions)]
# berth_id_list 记录了每个泊位对应的ID

for i in range(200):
    for j in range(200):
        if path_length[i][j] != 0:
            point_to_berth = '(%s,%s)' % (path_berth[i][j][0], path_berth[i][j][1])  # 某个坐标点对应的港口
            point_to_berth_id = int(berth_id_list[point_to_berth])
            if path_length[i][j] not in berth_length_times[point_to_berth_id]:
                berth_length_times[point_to_berth_id][path_length[i][j]] = 1
            else:
                berth_length_times[point_to_berth_id][path_length[i][j]] += 1

# %%
# 将berth_length_times转换为excel并输出
# 指定列名
columns = ['regionID', 'distance', 'times']

# 创建一个空的DataFrame
df = pd.DataFrame(columns=columns)

df['regionID'] = df['regionID'].astype(int)
df['distance'] = df['distance'].astype(int)
df['times'] = df['times'].astype(int)

for region_id in range(total_regions):
    for key, value in berth_length_times[region_id].items():
        new_row = {'regionID': region_id, 'distance': int(key), 'times': int(value)}
        df = df.append(new_row, ignore_index=True)

df.to_excel('%s.xlsx' % map_name, index=False)
