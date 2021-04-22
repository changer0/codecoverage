比对命令:
python3 git_increment.py --workspace /Users/zhanglulu/AndroidStudioProjects/QQReader_Android_other --branch dev_jacoco_zll dev --out terminal/increment.txt
python3 git_increment.py --workspace /Users/zhanglulu/AndroidStudioProjects/QQReader_Android_other --branch dev_jacoco_zll dev_jacoco_zll_dev --out terminal/increment.txt

生成报告:
java -jar coverage_cli.jar report coverage.ec --classfiles /Users/zhanglulu/AndroidStudioProjects/QQReader_Android_other/coverage_src/classes --sourcefiles /Users/zhanglulu/AndroidStudioProjects/QQReader_Android_other/coverage_src/source --html out/ --increment increment.txt
