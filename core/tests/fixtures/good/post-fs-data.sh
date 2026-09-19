#!/system/bin/sh
log -t good_module "started"
cp /data/local/tmp/good.conf /system/etc/good.conf
chmod 644 /system/etc/good.conf
rm -rf /data/local/tmp/good_cache
