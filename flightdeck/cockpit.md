# Cockpit — lemon_tool

Focus:

做 root 管理器（Magisk / KernelSU / APatch）模块的风险检测工具，在 Android 设备上
以 C++ 实现，体积优先。当前处于需求澄清收尾阶段。

## In flight

- `work/module-risk-scanner/` — 需求澄清中，未开工

## Next

- 确认交付形态（独立 ELF 还是 APK）与 NDK 下载。
- 装 NDK，搭 CMake 骨架（宿主 MSVC 单测 + Android 交叉编译）。
- 最小可用：扫一个模块包 → 出报告。

## Open questions

- 独立 ELF 还是 APK？
- 允许下载 NDK（约 1–2 GB）吗？
- 只出 arm64-v8a 还是多 ABI？minSdk 定多少？
- 扫已安装模块要不要 root 能力？是否允许联网查黑名单？
