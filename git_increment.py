# -*- coding: UTF-8 -*-
#正则
import re
import subprocess
import sys

def run_cmd(command):
    print("Run Cmd: ",command)
    subp = subprocess.Popen(command,shell=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE,encoding="utf-8", cwd=workspace)
    subp.wait()
    return subp.communicate()[0]

suffix_list = ["java", "kt"]
# 后缀是否可用
def suffix_enable(suffix):
    for s in suffix_list:
        if s == suffix:
            return True
    return False

#print("测试后缀: ", suffix_enable("asdf"))

def throw_exception():
    raise Exception("指令非法, 请参考: python3 git_increment.py --workspace /User/workspace --branch cur_branch compare_branch [--out increment.txt] ")

workspace_key = "--workspace"
workspace = ""
output_file_key = "--out"
output_file = "increment.txt"
branch_key = "--branch"
compare_branch = ""
cur_branch = ""
temp_branch = "temp"
#遍历取参数
for i in range(len(sys.argv)):
    if branch_key == sys.argv[i]:
        if i+1 >= len(sys.argv) or i+2 >= len(sys.argv):
            throw_exception()
        cur_branch = sys.argv[i + 1].strip()
        compare_branch = sys.argv[i + 2].strip()
    if output_file_key == sys.argv[i]:
        if i+1 >= len(sys.argv):
            throw_exception()
        output_file = sys.argv[i+1].strip()
    if workspace_key == sys.argv[i]:
        if i+1 >= len(sys.argv):
            throw_exception()
        workspace = sys.argv[i+1].strip()

if workspace == "":
    throw_exception()

if compare_branch == "" or cur_branch == "":
    throw_exception()

print("----------------")

print("参数: ")
print("工作空间:", workspace)
print("确认当前目录:", run_cmd("pwd").strip())
print("当前分支: ", cur_branch)
print("比较分支: ", compare_branch)
print("输出文件", output_file)

print("----------------")

cmd_git_fetch = "git fetch"
run_cmd(cmd_git_fetch)

# cmd_git_checkout_old_branch = "git checkout -b " + old_branch + " origin/" + old_branch

print("切换分支: ", temp_branch)
# 把原来本地分支删掉
run_cmd("git checkout -d " + temp_branch)
# 新建一个本地分支
run_cmd("git checkout -b " + temp_branch + " origin/" + cur_branch)
# 切换至当前本地分支
run_cmd("git checkout " + temp_branch)
print("拉取代码: ", temp_branch)
run_cmd("git pull")
run_cmd("git pull origin " + compare_branch)

print("----------------")

print("开始比对")
git_diff_result = run_cmd("git diff " + temp_branch + " origin/" + compare_branch)
searchObj = re.findall( '^diff --git (.*) ', git_diff_result, re.M | re.I)
# 测试代码
#searchObj = open("test_increment.txt", mode='r')
#print(cmd_result)
#print(searchObj.group(0))
prefix = "a/"

f = open(output_file, "w+")
for item in searchObj:
    #print("origin item: ", item.strip())
    all_len = len(item)
    prefix_len = len(prefix)
    #print("path_key len: ", key_len)
    item_split = item.split(".")
    if len(item_split) < 2:
        #如果发现没有后缀, 改文件忽略
        continue
    suffix = item.split(".")[1].strip()
    #print("发现后缀:",suffix)
    if not suffix_enable(suffix):
        # 如果发现不是可用后缀,就忽略
        continue
    find_index = item.find(prefix)
    if find_index < 0:
        continue
    #print("发现路径:", find_index)
    find_index = find_index + prefix_len
    result = item[find_index: all_len]
    print("增量文件:", result)
    #print(result)
    f.write(result + "\n")
# 关闭打开的文件
f.close()
print("处理完成(*^▽^*)")


#测试代码: python3 git_increment.py --workspace /Users/zhanglulu/AndroidStudioProjects/JaCoCoDemo --branch develop master --out file_test.txt