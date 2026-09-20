# Cockpit — lemon_tool

Focus:

做 root 管理器（Magisk / KernelSU / APatch）模块的风险检测工具：Android 端 APK，
C++ 核心 + MiuiX(Compose) 界面，只检查未安装的模块包。

## In flight

- `work/module-risk-scanner/` — 界面已按莫奈取色整体重写，CI 全绿（1.21 MiB，run 35477558871），等真机测试反馈

## Next

- 真机验证「选 zip → 出报告」整条链路（等用户测试）。
- 给 zip 条目数加上限（防内存被打爆）。
- 用真实模块样本压误报（现在的夹具是自造的，覆盖面有限）。
- 等真机截图判断新界面：顶部 40% 面板是否太空、中危黄框对比度是否够。

## Open questions

- 本机要不要也装 NDK + gradle（约 1.6 GB）？现状「CI 编 APK、本机编 C++ 跑测试」已经够用，
  暂不急。
- 要不要兼顾 32 位设备（armeabi-v7a）？现在只出 arm64-v8a。
