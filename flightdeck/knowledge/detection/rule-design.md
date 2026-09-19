# 检测规则设计原则：平铺关键词必然误报

SUMMARY: 平铺关键词匹配会把正常模块判成高危，让报告失去可信度。Always 分两层：
`core/src/rules.cpp` 的**特征表**一条命中只说明「模块干了这件事」，定级上限是中危；
`core/src/escalate.cpp` 的**升级表**只有「组合」与「路径敏感」两种情形才给高危。
新增规则前先想清楚它该进哪一层。
READ WHEN: before 新增或调整任何检测规则时。
RECHECK WHEN: 报告出现新的高频误报，或升级表加入新的判定维度之后。

---

## 证据（2026-09-20，自己的善意样本）

夹具 `core/tests/fixtures/good/` 是个完全正常的模块，只有一行
`rm -rf /data/local/tmp/good_cache`（删自己的临时缓存），结果：

```
风险统计：高危 1 · 中危 0 · 低危 0 · 信息 0
[高危] cmd.rm-rf  post-fs-data.sh:5   递归强制删除，可能清空系统或数据目录
```

同一套规则下，`chmod 755`、`mount -o`、`setprop`、`busybox`、`hosts` 这些**正常模块天天用**
的东西都会被报出来。一个对善意模块喊「高危」的扫描器，用户看两次就不看了。

## 两层模型（2026-09-20 已实现）

- **特征表** `core/src/rules.cpp`：一条命中 = 一条行为记录，只用于「行为清单」。定级上限是
  **中危**，且只有 `resetprop` / `setprop ro.` / `insmod` 这类真值得留意的才给中危，
  其余一律低危或信息。`cmd.rm-rf` 这种「风险取决于参数」的特征只给低危。
- **升级表** `core/src/escalate.cpp`：逐行判定，只有明确情形才给高危，每条是一个小函数：

  | 规则 id | 级别 | 判定条件 |
  |---|---|---|
  | `cmd.rm-rf-device` | 高 | `rm -r*` 的目标落在设备关键路径 |
  | `net.exec-download` | 高 | 同一行里既有 curl/wget 又管道进 shell |
  | `obf.exec-decode` | 高 | 同一行里既有 base64 -d / xxd -r 又管道进 shell |
  | `cmd.dd-block` | 高 | `dd` 且 `of=/dev/...` |
  | `cmd.resetprop-ro` | 高 | `resetprop` 且带 `ro.*` 参数 |
  | `obf.eval-dynamic` | 高 | **同一文件内**既有 eval 又有下载/解码 |
  | `cmd.rm-rf-adb` | 中 | `rm -r*` 打 `/data/adb`（常是模块自删，所以不给高危） |
  | `cmd.setenforce-off` | 中 | `setenforce 0` |
  | `cmd.chmod777-system` | 中 | `chmod 777` 打在设备关键路径 |
  | `cmd.remount-rw` | 中 | `remount` 打在设备关键路径 |

## 路径分档（升级表的地基）

`path_risk()` 把参数分三档；只认绝对路径，相对路径（`$MODDIR/...`）一律不判敏感：

- **不敏感**：`/data/local/tmp`、`/sdcard`、`/storage`、`/mnt`、`/data/media` —— 模块自己的地盘；
- **模块数据**（中危）：`/data/adb/...`；
- **设备关键**（高危）：`/`、`/*`、`/system`、`/vendor`、`/product`、`/odm`、`/apex`、`/sbin`、
  `/bin`、`/etc`、`/boot`、`/init`、`/data`。

## 报告必须给定性结论

`to_text` / `to_json` 都带 `verdict` 字段：高危 > 0 → 「建议不要安装」；只有中危 →
「未发现高危行为」；干净 → 「未发现高危或中危行为」。App 界面同样显示这一行。
误报率和检出率同等重要 —— 报告不能把二十条命中平铺出来让人自己判断。

## 回归防线（加规则前先跑）

- `core/tests/fixtures/good/`：善意基线，含 `mount -o bind`、`chmod 755`、`setprop persist`、
  `busybox`、`/etc/hosts`、`rm -rf /data/local/tmp/...`、`rm -rf $MODDIR/cache`。
  断言写死 **「高危 0 · 中危 0」**。
- `core/tests/fixtures/evil/`：断言 15 条特征 + 9 条升级规则全部命中。
