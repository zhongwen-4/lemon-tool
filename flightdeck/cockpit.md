# Cockpit — lemon_tool

Focus:

做 root 管理器（Magisk / KernelSU / APatch）模块的风险检测工具：Android 端 APK，
C++ 核心 + Java 壳，只检查未安装的模块包，体积优先。

## In flight

- `work/module-risk-scanner/` — 核心与宿主测试已完成，APK 与 CI 待验证

## Next

- 推到 GitHub 跑 workflow，确认 APK 出得来且小于 4 MiB。
- 真机验证「选 zip → 出报告」整条链路。
- 用真实模块样本调规则、压误报。

## Open questions

- 要不要为了本机也能构建 APK 而下载 NDK（约 1–2 GB）？
- 要不要兼顾 32 位设备（armeabi-v7a）？现在只出 arm64-v8a。
