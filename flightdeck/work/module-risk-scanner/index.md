# module-risk-scanner

状态：需求澄清中。已定：在 Android 设备上运行、C++、体积优先。目标：root 管理器
（Magisk / KernelSU / APatch）模块的风险检测工具。

## 已定决策

- 2026-09-20 运行位置：**在 Android 设备上跑**。
- 2026-09-20 技术栈：**C++**，CMake 构建。
- 2026-09-20 体积优先：产物尽量小 → `-Oz`/`-Os`、LTO、strip、`-fno-exceptions`
  `-fno-rtti`、只出一个 ABI、避免体积大的第三方库。
- 沿用原 MVP 的其余部分：吃模块 zip 与已解包目录；Magisk/KernelSU/APatch 统一处理；
  输出「分等级终端报告 + JSON」；检测规则声明式配置，加规则不改代码。

## 待确认（阻塞开工）

- 交付形态：独立静态 ELF（`adb push` 或终端里直接跑，最小约 100–300 KB）
  还是 APK（点开即用，但需要 Java/Kotlin 壳 + JNI，体积和复杂度都上去）
- 是否允许下载 NDK（约 1–2 GB；本机目前没有任何 Android C++ 工具链）
- ABI：只 arm64-v8a（推荐）还是兼顾 armeabi-v7a / x86_64
- minSdk：建议 24（Android 7）
- 是否需要 root：扫 `/data/adb/modules` 需要，只扫 zip 不需要
- 是否联网（黑名单、哈希信誉）

## 第一版检测维度

- 危险命令：mount / rm -rf / dd / chmod 777 / su / iptables
- 可疑网络地址：URL、IP、短链
- 混淆迹象：大块 base64、eval、hex blob
- 非预期可执行文件、ELF 架构与符号
- 开机钩子：post-fs-data.sh / service.sh / customize.sh 的实际行为
- 对 system/ 的覆盖范围、文件权限与 SELinux 上下文异常

## 下一步

1. 你回答「待确认」里的交付形态与 NDK 两点。
2. 装 NDK，搭 CMake 骨架：宿主 MSVC 跑单元测试 + Android 交叉编译。
3. 最小可用：扫一个模块包 → 出报告。

## 进度

- 2026-09-20 建档；同日定下 Android + C++ + 体积优先，并摸清本机工具链现状。

## Read now

- `knowledge/build/android-toolchain.md` — 开工前必读，本机工具链清单与坑

## Read if

- 需要 Android 模块规范细节（module.prop、脚本钩子、system 覆盖方式）→
  先建 `knowledge/android/` 下的条目再读
