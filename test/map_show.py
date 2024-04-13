# %%
"""
绘制地图信息
"""

import matplotlib.pyplot as plt
import numpy as np

# %%
# 输入数据
map_name_list = ['1', '2', '3', '4', '5', '6', '11', '22']
map_name = 'map' + map_name_list[6]  # 11->6 22->7
input_path_map = map_name + '.txt'  # 地图文件

with open(input_path_map) as file:
    content_map = file.readlines()

# %%
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
plt.scatter(map_land_y, map_land_x, s=s1, color='Gainsboro')  # 空地 .
plt.scatter(map_land_main_y, map_land_main_x, s=s1, color='NavajoWhite')  # 陆地主干道 >
plt.scatter(map_sea_y, map_sea_x, s=s1, color='SkyBlue')  # 海洋 *
plt.scatter(map_sea_main_y, map_sea_main_x, s=s1, color='DeepSkyBlue')  # 海洋主航道 ~
plt.scatter(map_wall_y, map_wall_x, s=s1, color='Black')  # 障碍 #
plt.scatter(map_berth_y, map_berth_x, s=s1, color='r')  # 泊位 B
plt.scatter(map_berth_area_y, map_berth_area_x, s=s1, color='MediumPurple')  # 靠泊区 K
plt.scatter(map_C_y, map_C_x, s=s1, color='Green')  # 海陆立体交通地块 C
plt.scatter(map_c_y, map_c_x, s=s1, color='LimeGreen')  # 海陆立体交通地块，同时为主干道和主航道 c
plt.scatter(map_lrobot_y, map_lrobot_x, s=s2, color='DarkOrange', marker='*')  # 机器人购买地块 R
plt.scatter(map_lboat_y, map_lboat_x, s=s2, color='Blue', marker='*')  # 船舶购买地块 S
plt.scatter(map_T_y, map_T_x, s=s2, color='DarkOrchid', marker='*')  # 交货点 T

plt.grid(True, color='k')
plt.xticks(np.arange(xmin, xmax, dx))  # x轴刻度
plt.yticks(np.arange(ymin, ymax, dy))  # y轴刻度
plt.tick_params(axis='x', labelsize=24)
plt.tick_params(axis='y', labelsize=24)
plt.tight_layout()  # 可选，自动调整布局
plt.savefig('%s.png' % map_name)
plt.show()
