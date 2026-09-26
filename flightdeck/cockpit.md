# Cockpit — lemon_tool

Focus:

做 root 管理器（Magisk / KernelSU / APatch）模块的风险检测工具：Android 端 APK，
C++ 核心 + MiuiX(Compose) 界面，只检查未安装的模块包。

## In flight

- `work/module-risk-scanner/` — 界面照 SukiSU Ultra 重画：**移植了它的悬浮底栏**（含 DampedDragAnimation，
  按用户定论去掉毛玻璃那一路）与 MiuiX 行组件，三个页面（主页 / 检查历史 / 设置）改成它的版式
  （整卡英雄状态卡 + 一页一个 Scaffold/TopAppBar + 12dp 卡片流）；版本 **0.4.0/code5**；
  **本机 debug/release 编译通过**，等 CI 出包与真机看观感。

## Next

- 真机验证「选 zip → 出报告」整条链路，并看新版式（等用户测试）：英雄卡实际高度与溢出图标的裁切、
  底栏胶囊拖动切页的手感、MiuiX 行组件在卡片里的间距、分区标题要不要保留（SukiSU 版式里没有）。
- 给 zip 条目数加上限（防内存被打爆）。
- 用真实模块样本压误报（现在的夹具是自造的，覆盖面有限）。
- 若要毛玻璃：`miuix-blur` 已在依赖里，但要补 liquid / `InteractiveHighlight` 那一层
  （约 25 KB，且要处理 API 33 的 `RuntimeShader` 与本项目 minSdk 24 的冲突）。

## 已落地（倒序；细节都在 work 包里）

- 2026-09-27：移植 SukiSU 的界面代码 —— 只搬组件与骨架、只做 Miuix 一套、图标继续用
  `material-icons-extended`；三页按它的版式重画；GPL-3.0 义务已履行（每个搬进来的文件写出处与改动日期，
  它的启动图标一个字没搬）。commit `a05fdbb`。
- 2026-09-27 之前：工具链升到 **AGP 9.4.1 / Kotlin 2.4.20 / Gradle 9.7.1 / JDK 21 /
  compose-bom 2026.09.00 / MiuiX 0.9.4**（与 SukiSU 完全同一套）。所以 cockpit 里原来那条
  「要不要升 MiuiX 0.9.4 换取 preference 行组件」**已经做完**；本机与 CI 的 Gradle 分叉
  （本地 9.2.0 / CI 8.13）也一并消失，两边都是 9.7.1 + JDK 21。
- 2026-09-25：GPL-3.0 声明 + 根目录 `LICENSE`；CI 发布签名与权限修复；三 tab 底栏 + 检查历史。
- 更早的（核心、CI、莫奈取色、图标、主题、误报治理）见 `work/module-risk-scanner/index.md`。

## Open questions

- 用户提「做个虚拟环境直接刷写模块看执行了哪些命令」：动态沙箱**进不了 APK**（已在本机实测假 PATH
  dry-run 并论证，见 `knowledge/detection/dry-run-sandbox.md`）。剩下的选择题：要不要在 APK 里做**静态**
  执行轨迹预览（按钩子分组列命令、零执行）？要不要另开一个 work 包做桌面/CI 侧 sandbox？
- 更新检查现在只查 `api.github.com`，**国内网络常常不通**（界面只能报检查失败）。要不要加镜像回退
  （比如 jsDelivr 读仓库里的 `version.json`）？
- 本机要不要也装 NDK + gradle（约 1.6 GB）？现状「CI 编 APK、本机编 C++ 与 Kotlin 跑测试」已经够用。
- 要不要兼顾 32 位设备（armeabi-v7a）？现在只出 arm64-v8a。
- 分区标题（「模块检查 / 模块信息 / 存储 / 关于」这些）是你 2026-09-25 要的，而 SukiSU 的版式里没有 ——
  这次先保留着，等你说去留。
