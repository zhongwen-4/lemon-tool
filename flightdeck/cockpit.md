# Cockpit — lemon_tool

Focus:

做 root 管理器（Magisk / KernelSU / APatch）模块的风险检测工具：Android 端 APK，
C++ 核心 + MiuiX(Compose) 界面，只检查未安装的模块包。

## In flight

- `work/module-risk-scanner/` — 核心与宿主测试已完成，MiuiX 界面已写，APK 构建待 CI 验证

## Next

- 推到 GitHub 跑 workflow，确认 APK 出得来（体积守卫已放宽到 12 MiB）。
- 真机验证「选 zip → 出报告」整条链路。
- 用真实模块样本调规则、压误报。

## Open questions

- 要不要为了本机也能验证 APK 构建而下载 NDK + gradle（约 1.6 GB）？
  不下载的话，Kotlin/Compose 那条链第一次真编译只能发生在 CI。
- 要不要兼顾 32 位设备（armeabi-v7a）？现在只出 arm64-v8a。
