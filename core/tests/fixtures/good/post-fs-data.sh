#!/system/bin/sh
MODDIR=${0%/*}
log -t good_module "started"
cp /data/local/tmp/good.conf $MODDIR/system/etc/good.conf
chmod 644 $MODDIR/system/etc/good.conf
rm -rf /data/local/tmp/good_cache
