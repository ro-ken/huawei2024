# %%
"""
从debug中提取海洋路径
输出为主程序setSeaPath标准格式
"""
import re

# %%
# 输入数据
map_name_list = ['1', '2', '3', '11', '22']
map_id = map_name_list[4]
debug_path = 'sea_path' + map_id + '.txt'  # 选择地图 0-->map1 1-->map2 2-->map3

with open(debug_path) as file:
    content_path = file.readlines()


# %%
# 定义一个函数，接收一行文本作为参数，并返回提取出的数字列表
def extract_numbers(line):
    # 使用正则表达式匹配数字模式
    numbers = re.findall(r'\b\d+\.*\d*\b', line)
    # 返回匹配到的数字列表
    return numbers


# %%
# 格式转换
# setSeaPath(new Point(36,98),3,new Point(50, 170),"[(36,98), (37,98), (38,98), (49,170)]");
sea_path = []
for i in range(len(content_path)):
    if '方向' in content_path[i]:
        dest_index = content_path[i].index('dest:')
        direction = int(content_path[i][dest_index - 1])
        num = extract_numbers(content_path[i])
        numbers = list(map(int, num))
        path_format = 'setSeaPath(new Point({},{}),{},new Point({}, {}),"{}");'
        path = path_format.format(numbers[0], numbers[1], direction, numbers[2], numbers[3], content_path[i + 1][5:-1])
        sea_path.append(path)

# %%
# 输出文件
output_name = 'setSeaPath' + map_id + '.txt'

with open(output_name, "w") as file:
    for path in sea_path:
        file.write(path + "\n")
