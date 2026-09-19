# module-risk-scanner

状态：需求澄清中，尚未开始实现。目标：root 管理器（Magisk / KernelSU / APatch）
模块的风险检测工具。

## 下一步

1. 回答「待确认」里的形态与范围问题，定下 MVP。
2. 需要界面的话跑 `$impeccable init` 落 `PRODUCT.md`。
3. 建最小骨架：扫描一个模块包 → 输出风险报告。

## 进度

- 2026-09-20 建档。项目由你提出：写一个 root 管理器模块风险检测工具。
  仓库已 `git init`，deck 与本文件进了首个提交。

## 待确认（阻塞开工）

- 扫描对象：模块 zip 包（安装前）／设备上已安装的模块目录／两者都要
- 运行位置：PC 离线分析／Android 设备上／都要
- 形态：CLI／桌面 GUI／本地网页
- 技术栈：Python／Node+TS／Go／Rust
- 支持范围：Magisk／KernelSU／APatch／全部
- 检测项优先级，以及是否允许联网（黑名单、哈希信誉）

## Read now

（无）

## Read if

- 开工前需要 Android 模块规范细节 → 建 `knowledge/android/` 下的条目再读
