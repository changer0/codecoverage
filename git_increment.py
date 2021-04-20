# -*- coding: UTF-8 -*-
#正则
import re
import subprocess
import sys

def run_cmd(command):
    print("Run Cmd: ",command)
    subp = subprocess.Popen(command,shell=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE,encoding="utf-8")
    # subp.wait(2)
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
    raise Exception("指令非法, 请参考: python3 git_increment.py --branch develop master [--out increment.txt] ")

output_file_key = "--out"
output_file = "increment.txt"
branch_key = "--branch"
old_branch = ""
new_branch = ""
for i in range(len(sys.argv)):
    if branch_key == sys.argv[i]:
        if i+1 >= len(sys.argv) or i+2 >= len(sys.argv):
            throw_exception()
        new_branch = sys.argv[i+1].strip()
        old_branch = sys.argv[i+2].strip()
    if output_file_key == sys.argv[i]:
        if i+1 >= len(sys.argv):
            throw_exception()
        output_file = sys.argv[i+1].strip()


if old_branch == "" or new_branch == "":
    throw_exception()

print("获取到的参数: ")
print("当前分支: ", new_branch)
print("比较分支: ", old_branch)
print("输出文件", output_file)

cmd_git_fetch = "git fetch"
run_cmd(cmd_git_fetch)

# cmd_git_checkout_old_branch = "git checkout -b " + old_branch + " origin/" + old_branch
cmd_git_checkout_new_branch = "git checkout -b " + new_branch + " origin/" + new_branch
print("开始切换分支: ", new_branch)
run_cmd(cmd_git_checkout_new_branch)

print("开始比对")
git_diff_result = run_cmd("git diff origin/"+ old_branch + " origin/" + new_branch)
searchObj = re.findall( '^diff --git (.*) ', git_diff_result, re.M | re.I)
# 测试代码
#searchObj = open("test_increment.txt", mode='r')
#print(cmd_result)
#print(searchObj.group(0))
prefix = "/java/"

f = open(output_file, "w+")
for item in searchObj:
    print("origin item: ", item.strip())
    all_len = len(item)
    key_len = len(prefix)
    #print("path_key len: ", key_len)
    item_split = item.split(".")
    if len(item_split) < 2:
        #如果发现没有后缀, 改文件忽略
        continue
    suffix = item.split(".")[1].strip()
    print("发现后缀:",suffix)
    if not suffix_enable(suffix):
        # 如果发现不是可用后缀,就忽略
        continue
    find_index = item.find(prefix)
    if find_index < 0:
        continue
    print("发现路径:", find_index)
    find_index = find_index + key_len
    result = item[find_index: all_len].split(".")[0]
    #print(result)
    f.write(result + "\n")
# 关闭打开的文件
f.close()