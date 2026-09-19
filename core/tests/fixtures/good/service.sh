#!/system/bin/sh
MODDIR=${0%/*}
mount -o bind $MODDIR/system/etc/hosts /system/etc/hosts
chmod 755 $MODDIR/system/bin/helper
setprop persist.good_module.ready 1
busybox sh $MODDIR/scripts/clean.sh
rm -rf $MODDIR/cache
