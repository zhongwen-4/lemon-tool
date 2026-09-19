# Cockpit — lemon_tool

Focus:

做 root 管理器（Magisk / KernelSU / APatch）模块的风险检测工具。
当前处于需求澄清阶段，形态与检测范围未定。

## In flight

- `work/module-risk-scanner/` — 需求澄清中，未开工

## Next

- 定下形态（CLI／GUI／网页）、运行位置（PC／设备）、技术栈与检测项范围。
- 需要界面则跑 `$impeccable init` 落 `PRODUCT.md`。
- 建最小骨架：扫描一个模块包 → 输出风险报告。

## Open questions

- 扫描对象是模块 zip（安装前）、设备上已安装模块目录，还是两者？
- 在 PC 上离线和在设备上运行，选哪个（还是都要）？
- 技术栈选什么？
- 是否允许联网（黑名单、哈希信誉）？
