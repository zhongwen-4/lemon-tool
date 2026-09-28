# Cockpit — lemon_tool

Focus:

做 root 管理器（Magisk / KernelSU / APatch）模块的风险检测工具：Android 端 APK，
C++ 核心 + MiuiX(Compose) 界面，只检查未安装的模块包。

## In flight

- `work/module-risk-scanner/` — **三页都接上真实数据了，等真机验收**。
  三页口径用户已在 2026-09-28 定死并落地：主页 = 「点此开始检测」+ **扫描结果**（结论卡 + 逐条发现）
  +「应用版本」+「提交 BUG」；检查历史 = **本项目自己的扫描记录**（一行一条 JSONL 存 `filesDir`，
  上限 200），骨架仍是移植来的 SU 日志列表、顶栏标题已改成「检查历史」；设置 = 关于 + 检查更新。
  取色走 SukiSU 的**非莫奈**那一支。版本 **0.7.0/code8**、commit `3ffed5b` 已推，
  **CI run 30 两个 job 全绿（已出包）**。本机 8 条出包前置全过。
  **2026-09-28 又补一轮**：关于页接系统返回键 + 搬上游 OS3 动态背景（新增 `miuix-shader-android:0.9.4`），
  顺带把 `MiuixTheme` 抽成 `MiuixAppTheme`，让关于页也在主题里（原先它落在默认浅色上）。版本
  **0.8.0/code9**、commit `08c4cb4` 已推，**CI run `36365506193` 两个 job 全绿（已出包）**。
  下一件事：装真机走一遍全链路（选 zip → 结论卡 → 发现列表 → 检查历史 → 详情 → 清空），**外加关于页**。

## Next

- **真机验收**：重点看结论卡长文本换行、发现卡等级配色在深色下是否可读、条目卡在大字号下的裁切、
  清空确认弹窗的按钮宽度。扫描本身只在真机验证过「选 zip → 出报告」，这轮补上界面显示后要重走一遍。
- 检查历史的周边（用户没要，先记着）：搜索 / 按等级筛选 / 单条删除 / 导出报告。
- **关于页的观感（本轮新增）**：渐变背景**只有 Android 15+ 才有**（上游门控 `SDK >= 35`，低版本退化成窗口
  底色，不是 bug）；真机顺带看：返回键是回主页而不是退出、Logo 区滚动淡出、深色下关于页底色不再发白。
- 真机看三页观感：英雄卡实际高度与溢出图标的裁切、底栏胶囊拖动切页的手感、MiuiX 行组件在卡片里的间距。
- 给 zip 条目数加上限（防内存被打爆）。
- 用真实模块样本压误报（现在的夹具是自造的，覆盖面有限）。
- 若要毛玻璃：**这条路现在被 minSdk 卡死** —— `miuix-blur-android:0.9.4` 的 manifest 硬要求
  minSdk 33，而本项目是 24（2026-09-27 已把该依赖删掉）。要做只能整体抬 minSdk，或自己实现模糊。

## 已落地（倒序；细节都在 work 包里）
- 2026-09-28：**关于页修返回键 + 搬上游 OS3 动态背景** —— `AboutScreen` 补 `BackHandler`（上游靠 navigation3
  的路由栈，本项目没有导航库，不接就直接退出 App）；effect 包整包搬入、RuntimeShader 改走新的
  `miuix-shader-android:0.9.4`（minSdk 24；上游的 `miuix-blur` 被 minSdk 33 卡死），门控照上游 `SDK >= 35`；
  顺带把 `MiuixTheme` 抽成 `MiuixAppTheme`，修掉「关于页落在 `lightColorScheme()` 默认值、深色下发白」。
  版本 0.8.0/code9，commit `08c4cb4`。两条新知识：`android/about-bg-effect-shader.md`、
  `android/miuix-theme-scope.md`。
- 2026-09-28：**主页 / 设置页精简收尾 + 去掉莫奈取色** —— 取色改走 `MiuixKernelSUTheme` 的非莫奈那一支；
  主页只留「点此开始检测 / 应用版本 / 提交 BUG」（状态卡的另两支与 SELinux + Seccomp 那张卡全删），
  设置页只留「检查更新 / 关于」，顺带删掉 `strings.xml` 里 79 条已无引用的文案。版本 0.6.0/code7，
  commit `c14ecbb`。三条知识：`android/theme-non-monet-sukisu.md`（新增）、
  `tooling/github-push-and-local-proxy.md`（改名并**推翻**昨天的结论：真凶是本机 7890 系统代理，
  PowerShell 走它、git 不走）、`tooling/file-edit-anchors-and-newlines.md`（补记切片端点差一位的第二次失手）。
- 2026-09-28：**实现检查模块与检查历史** —— 主页新增 `ScanSummarySection`（检测中转圈 / 失败卡 /
  结论卡）与逐条发现卡（走 LazyColumn 独立 item，懒加载）；新增 `ScanHistory.kt`（`filesDir` 里一行一条
  JSONL，上限 200），映射层把记录变成 SU 日志列表能渲染的条目、键名走 `ScanEntryFields` 两边共用；
  顶栏标题由「SU 日志」改成「检查历史」，补空态提示与「清空检查历史」（先弹确认）。版本 0.7.0/code8，
  commit `3ffed5b`。三条知识：`android/scan-history-data-source.md`（新增）、`android/miuix-0.9.4.md`
  补「查组件签名的三条路」、`tooling/github-push-and-local-proxy.md` 补「代理会被随时开关」，
  另在 `tooling/file-edit-anchors-and-newlines.md` 记下「脚本报 OK 但文件没变」出现过两次。
  推送这条补全了：代理关掉后 DNS 解到被墙 IP 时，`git -c http.curloptResolve=github.com:443:<当下通的IP>`
  一句就能推（不必起隧道），前提是推之前先挑一个当时真的通的 IP。
- 2026-09-28：**界面整包换成 SukiSU 的骨架**（UI 口径）—— 主页 / 关于 / 设置照搬结构与尺寸、
  检查历史换成 SU 日志列表、三页数据全是空状态，`History.kt` 删掉，`ScanUi.kt` 重写（1041 → 358 行）。
  版本 0.5.0/code6，commit `8517d3f`。两个坑进了知识库：here-string 丢换行把 `package` 粘进注释
  （`knowledge/tooling/here-string-package-line.md`）、推 GitHub 连不上（即为上面那条，当时结论有误）。
- 2026-09-27：**修掉 CI 出包失败**（run `36281282791`）—— 升 MiuiX 0.9.4 时 compileSdk 留在 36，
  `apk` job 卡死在 `checkReleaseAarMetadata`；改成 `compileSdk 37` + `compileSdkMinor 2`
  （SDK 平台从 37 起按小版本发布，用 `android-37.2`；targetSdk 不动），并删掉硬要求 minSdk 33 的
  `miuix-blur` 依赖。commit `6af075b`，**CI 复跑（run `36282815228`）两个 job 全绿、出包成功**；
  详见 `knowledge/build/compile-sdk-and-aar-metadata.md`。
- 2026-09-27：移植 SukiSU 的界面代码 —— 只搬组件与骨架、只做 Miuix 一套、图标继续用
  `material-icons-extended`；三页按它的版式重画；GPL-3.0 义务已履行（每个搬进来的文件写出处与改动日期，
  它的启动图标一个字没搬）。commit `a05fdbb`。
- 2026-09-27 之前：工具链升到 **AGP 9.4.1 / Kotlin 2.4.20 / Gradle 9.7.1 / JDK 21 /
  compose-bom 2026.09.00 / MiuiX 0.9.4**（与 SukiSU 完全同一套）。所以原来那条
  「要不要升 MiuiX 0.9.4 换取 preference 行组件」**已经做完**；本机与 CI 的 Gradle 分叉也一并消失。
- 2026-09-25：GPL-3.0 声明 + 根目录 `LICENSE`；CI 发布签名与权限修复；三 tab 底栏 + 检查历史。
- 更早的（核心、CI、莫奈取色、图标、主题、误报治理）见 `work/module-risk-scanner/index.md`。

## Open questions

- 关于页的动态背景被上游卡在 **Android 15+**（`SDK >= 35`），而 `RuntimeShader` 从 API 33 就有 ——
  要不要把门槛降到 33，让 Android 13/14 也能看到渐变？（先按上游口径做了，等真机反馈再定）

- 用户提「做个虚拟环境直接刷写模块看执行了哪些命令」：动态沙箱**进不了 APK**（已在本机实测假 PATH
  dry-run 并论证，见 `knowledge/detection/dry-run-sandbox.md`）。剩下的选择题：要不要在 APK 里做**静态**
  执行轨迹预览（按钩子分组列命令、零执行）？要不要另开一个 work 包做桌面/CI 侧 sandbox？
- 更新检查现在只查 `api.github.com`，**国内网络常常不通**（界面只能报「检查失败」）。要不要加镜像回退
  （比如 jsDelivr 读仓库里的 `version.json`）？
- 设置页那个「检查更新」开关现在是本页自持（`rememberSaveable`，默认开），**没有接真正的自动检查逻辑**；
  要不要接上（进主页时后台检查 + 有新版就显示 UpdateCard）？
- 本机要不要也装 NDK + gradle（约 1.6 GB）？现状「CI 编 APK、本机编 C++ 与 Kotlin 跑测试」已经够用。
- 要不要兼顾 32 位设备（armeabi-v7a）？现在只出 arm64-v8a。
