# %%
"""
绘制海洋路径信息
"""
import random
import re
import matplotlib.pyplot as plt
import numpy as np

# %%
# 输入数据
map_name_list = ['0', '2', '3', '11', '22']
map_name = map_name_list[4]  # 选择地图 0-->map1 1-->map2 2-->map3
input_map = '../maps/map' + map_name + '.txt'  # map文件存放路径
input_path = 'setSeaPath' + map_name + '.txt'  # sea_path文件存放路径
save_all_sea_path = True  # 是否保存全部海上路径（全部路径一图）
save_single_sea_path = True  # 事后保准单条海上路径（一路径一图）

with open(input_map) as file:
    content_map = file.readlines()

with open(input_path) as file:
    content_path = file.readlines()

# %%
# 处理原始地图信息
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
# 处理海洋路径信息
# 定义一个函数，接收一行文本作为参数，并返回提取出的数字列表
def extract_numbers(line):
    # 使用正则表达式匹配数字模式
    numbers = re.findall(r'\b\d+\.*\d*\b', line)
    # 返回匹配到的数字列表
    return numbers


start_dest_end = []  # 存放路径起点、方向、终点信息 [起点x,起点y,方向,终点x,终点y]
sea_path_x = []  # 存放所有海洋路径的x
sea_path_y = []  # 存放所有海洋路径的y

for i in range(len(content_path)):
    path_x_single = []
    path_y_single = []
    info = content_path[i].split('"')
    info_start_dest_end = list(map(int, extract_numbers(info[0])))
    info_path = extract_numbers(info[1])
    start_dest_end.append(info_start_dest_end)
    for j in range(len(info_path)):
        if j % 2 == 0:
            path_x_single.append(int(info_path[j]))
        else:
            path_y_single.append(int(info_path[j]))
    sea_path_x.append(path_x_single)
    sea_path_y.append(path_y_single)

# %%
# 控制网格参数
xmin = 0
xmax = 200.5
dx = 10
ymin = 0
ymax = 200.5
dy = 10


# 随机生成颜色
def rgbrandom():
    rr = random.randint(0, 255)
    gg = random.randint(0, 255)
    bb = random.randint(0, 255)
    rgb = str(rr) + ',' + str(gg) + ',' + str(bb)
    return rgb


# 颜色转化为16进制
def RGB_to_Hex(rgb):
    RGB = rgb.split(',')  # 将RGB格式划分开来
    mycolor = '#'
    for i in RGB:
        num = int(i)
        # 将R、G、B分别转化为16进制拼接转换并大写  hex() 函数用于将10进制整数转换成16进制，以字符串形式表示
        mycolor += str(hex(num))[-2:].replace('x', '0').upper()
    return mycolor


# %%
# 绘制全部路线图
if save_all_sea_path:
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

    # 绘制海洋路线
    colors = ['lightcoral', 'orange', 'yellow', 'lawngreen', 'cyan', 'deepskyblue', 'blueviolet', 'pink', 'deeppink',
              'blue']

    for i in range(len(start_dest_end)):
        plt.plot(sea_path_y[i], sea_path_x[i], linewidth=5, color=RGB_to_Hex(rgbrandom()))  # 全部海洋路径

    plt.grid(True, color='k')
    plt.xticks(np.arange(xmin, xmax, dx))  # x轴刻度
    plt.yticks(np.arange(ymin, ymax, dy))  # y轴刻度
    plt.tick_params(axis='x', labelsize=22)
    plt.tick_params(axis='y', labelsize=22)
    plt.tight_layout()  # 可选，自动调整布局
    filename = '%s_all_sea_path.png' % map_name
    print(filename)
    plt.savefig('%s' % filename)
    # plt.show()
    plt.close()

# %%
# 绘制单条路线图
if save_single_sea_path:
    for i in range(len(start_dest_end)):
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
        plt.scatter(map_sea_y, map_sea_x, s=s1, color='SkyBlue', alpha=0.4)  # 海洋 *
        plt.scatter(map_sea_main_y, map_sea_main_x, s=s1, color='DeepSkyBlue', alpha=0.4)  # 海洋主航道 ~
        plt.scatter(map_wall_y, map_wall_x, s=s1, color='Black')  # 障碍 #
        plt.scatter(map_berth_y, map_berth_x, s=s1, color='r', alpha=0.6)  # 泊位 B
        plt.scatter(map_berth_area_y, map_berth_area_x, s=s1, color='MediumPurple', alpha=0.4)  # 靠泊区 K
        plt.scatter(map_C_y, map_C_x, s=s1, color='Green', alpha=0.6)  # 海陆立体交通地块 C
        plt.scatter(map_c_y, map_c_x, s=s1, color='LimeGreen', alpha=0.6)  # 海陆立体交通地块，同时为主干道和主航道 c
        plt.scatter(map_lrobot_y, map_lrobot_x, s=s2, color='DarkOrange', marker='*')  # 机器人购买地块 R
        plt.scatter(map_lboat_y, map_lboat_x, s=s2, color='Blue', marker='*')  # 船舶购买地块 S
        plt.scatter(map_T_y, map_T_x, s=s2, color='DarkOrchid', marker='*')  # 交货点 T

        # 绘制海洋路线
        colors = ['lightcoral', 'orange', 'yellow', 'lawngreen', 'cyan', 'deepskyblue', 'blueviolet', 'pink',
                  'deeppink', 'blue']

        plt.scatter(start_dest_end[i][1], start_dest_end[i][0], s=s2, color='r', marker='*')  # 起点
        plt.scatter(start_dest_end[i][4], start_dest_end[i][3], s=s2, color='blue', marker='*')  # 终点
        plt.scatter(sea_path_y[i], sea_path_x[i], s=50, color='DarkOrange')  # 海洋路径

        plt.grid(True, color='k')
        plt.xticks(np.arange(xmin, xmax, dx))  # x轴刻度
        plt.yticks(np.arange(ymin, ymax, dy))  # y轴刻度
        plt.tick_params(axis='x', labelsize=22)
        plt.tick_params(axis='y', labelsize=22)
        plt.tight_layout()  # 可选，自动调整布局
        filename = '%s-(%s,%s)-%s-(%s,%s).png' % (
            i+1, start_dest_end[i][0], start_dest_end[i][1], start_dest_end[i][2], start_dest_end[i][3],
            start_dest_end[i][4])
        print(filename)
        plt.savefig('G:\\Python_file\\Pycharm_project\\Huawei\\2024Huawei\\SemiFinal\\util\\map%s_sea_path\\%s' % (
            map_name, filename))
        # plt.show()
        plt.close()
