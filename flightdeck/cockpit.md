# Cockpit — lemon_tool

Focus:

做 root 管理器（Magisk / KernelSU / APatch）模块的风险检测工具：Android 端 APK，
C++ 核心 + MiuiX(Compose) 界面，只检查未安装的模块包。

## In flight

- `work/module-risk-scanner/` — **三页都接上真实数据了，等真机验收**。
  三页口径用户已在 2026-09-28 定死并落地：主页 = 「点此开始检测」+ **扫描结果**（结论卡 + 逐条发现）
  +「应用版本」+「提交 BUG」；检查历史 = **本项目自己的扫描记录**（一行一条 JSONL 存 `filesDir`，
  上限 200），骨架仍是移植来的 SU 日志列表、顶栏标题已改成「检查历史」；设置 = 关于 + 检查更新+ 模糊 + 液态玻璃（后两行是 2026-09-29 补的显示开关）。
  取色走 SukiSU 的**非莫奈**那一支。版本 **0.7.0/code8**、commit `3ffed5b` 已推，
  **CI run 30 两个 job 全绿（已出包）**。本机 8 条出包前置全过。
  **2026-09-28 又补一轮**：关于页接系统返回键 + 搬上游 OS3 动态背景（新增 `miuix-shader-android:0.9.4`），
  顺带把 `MiuixTheme` 抽成 `MiuixAppTheme`，让关于页也在主题里（原先它落在默认浅色上）。版本
  **0.8.0/code9**、commit `08c4cb4` 已推，**CI run `36365506193` 两个 job 全绿（已出包）**。
  下一件事：装真机走一遍全链路（选 zip → 结论卡 → 发现列表 → 检查历史 → 详情 → 清空），**外加关于页**。
  **2026-09-29 补一轮（用户报的四件事）**：磨砂玻璃 + 液态玻璃落地（接 `miuix-blur:0.9.4`，
  靠 `tools:overrideLibrary` 绕开它的 minSdk 33；液态玻璃 = 上游那枚会折射/会跟手倾斜发亮的胶囊），
  关于页的返回手势换成 `PredictiveBackHandler`（页面跟着手指滑出），关于页背景门槛从上游的 `SDK >= 35`
  降到 API 33、更低版本补一层静态渐变；**同时破了一个案**：用户说的「返回退桌面 / 背景没实现 /
  模块检查没实现」是 **0.6.0 的功能集** —— 手机上装的根本不是新包（分支构建每次用随机钥匙签名，
  覆盖安装必失败）。CI 改成用仓库里固定的 `ci-signing/mrs-ci.jks`，产物改名带版本号。版本
  **0.9.0/code10**、commit `9cc8a86`，CI run `36490628242` 两个 job 全绿（产物 `mrs-apk-0.9.0-c10-9cc8a86`）。
  **下一件事：让用户先卸载旧版再装 0.9.0，在主页核对「应用版本」。**
  **2026-09-29 第二轮（用户：把预测性返回手势加个开关）**：设置页补上游同名行（文案照抄上游 zh-CN），
  状态进 `DisplaySettings.enablePredictiveBack`（默认开）；开关直接管关于页那一处：开 = `PredictiveBackHandler`
  （跟手滑出）、关 = `BackHandler`（照常回上一页、页面不动画），两条只挂一条。**没照上游的应用级机制**
  （上游是 API 34+ 用隐藏 API 翻平台标志 + `org.lsposed.hiddenapibypass`，重启才生效、只影响平台动画）——
  本项目没引那层依赖，预测性返回也只有关于页一处。manifest 同时补 `android:enableOnBackInvokedCallback="true"`
  （配 `tools:targetApi="33"`），否则 API 33/34 收不到进度回调、跟手动画出不来。版本 **0.10.0/code11**、
  commit `5ce94a1`，本机 8 条出包前置全过，CI run `36494192545`。**下一件事仍是真机验收**，
  外加：设置页核对四行开关、关掉预测性返回看关于页返回是不是不再跟手。
  **2026-09-29 第三轮（用户：结果全进检查历史 / 主页只留三样 / 检测卡按风险红黄绿）**：
  主页只剩检查卡 + 应用版本 + 提交 BUG（逐条发现卡与详细结论卡删掉）；检查卡跟着 `ScanState` 变脸，
  出结果后整卡按最高风险着色（高危红 / 中危黄 / 其余绿），版式照上游主页「工作中」那张卡
  （大字结论 + 一行计数 + 左下模块名 + 右下 110dp 大图标）；结果明细移进「检查历史」的条目详情
  （`SulogEntry.extraDetail` + `ScanUi.scanRecordDetail`）。版本 **0.11.0/code12**，本机 8 条出包前置全过。
  **推送状态**（2026-09-29 第五轮更正）：上面那串 commit——`b7d7bf5` / `8ced491` 与两笔 flightdeck
  回写（`58a4b2d` / `8ccb392`）——**后来都推上去了**，远端 CI run `36498756064` 两个 job 全绿
  （产物 `mrs-apk-0.11.1-c13-8ced491`）。
  **2026-09-29 第四轮（用户真机反馈）**：结论卡文字重叠（我上一轮三层 `fillMaxSize` 叠一格的锅）
  → 装饰图标层改 `matchParentSize()`、内容层单列自定高、模块名给图标留 96dp + 省略号；卡片标题改成
  直接写「高危 / 中危 / 低危」（一档都没有才写「未发现风险」）；检查历史条目详情改成 SU 日志那套
  「key: value」逐行写法（发现逐条：等级 / 规则 / 文件 / 说明）。版本 **0.11.1/code13**，本机 8 条全过。
  新知识 `android/compose-box-corner-layout.md`。
  **2026-09-29 第五轮（用户：结论卡标题只写「xx模块」）**：标题文案拆成 `scan_result_high/medium/low`
  （高危模块 / 中危模块 / 低危模块），卡片按最高档取用；**一档发现都没有时也按最低那档写**
  （用户口径是「只写这三档」），`scan_no_findings` 保留给那一行计数用。版本 **0.11.2/code14**、
  commit `0becacd` 已推，本机 8 条前置全过。坑：PowerShell `@(@($a,$b))` 会**拍平**成两元素数组
  （`$x[0]` 变成首字符，锚点命中数假报 687），已补进
  `knowledge/tooling/file-edit-anchors-and-newlines.md` 的「第六次」。
  **2026-09-29 第六轮（用户：你改一下）**：标题保持三档不动，把零发现时那一行计数从「未发现风险」改成
  「未发现风险项」——标题是「低危模块」、下面又来一句「未发现风险」，两句叠着看像自己打自己。
  版本 **0.11.3/code15**、commit `811bca3`，本机 8 条前置全过。**这一笔没推上去**：连试两个 IP
  （先 `Failed to connect ... 443`、后 `Recv failure: Connection was reset`），按规矩没再试第三次；
  **推送补记**：隔一轮再试，第一下就成功了（`aca60ed..488a6f2`）—— 这条线是「挑 IP + 看运气」，
  失败两次就停手、下一轮再试，是可行的节奏。
  **2026-09-29 第七轮（用户：右侧写点击查看详情）**：检查历史每条右侧的状态由「结论」改成
  「点击查看详情」（新增 `scan_entry_view_detail`），结论挪进详情弹窗的 fields；映射层注释跟着改。
  版本 **0.11.4/code16**、commit `488a6f2` 已推，本机 8 条前置全过。
  **未决（问过用户、还没答）**：条目要不要把「名称 / 路径 / 时间 / 标签」四样显式写出来，以及第二行
  要不要真路径（SAF 只给 `content://` URI，拿不到 `/sdcard/...` 那种真路径）。

## Next

- **真机验收**：重点看结论卡长文本换行、发现卡等级配色在深色下是否可读、条目卡在大字号下的裁切、
  清空确认弹窗的按钮宽度。扫描本身只在真机验证过「选 zip → 出报告」，这轮补上界面显示后要重走一遍。
- 检查历史的周边（用户没要，先记着）：搜索 / 按等级筛选 / 单条删除 / 导出报告。
- **关于页的观感**：渐变背景 2026-09-29 起 **Android 13（API 33）以上都有**（门槛从上游的 `SDK >= 35` 降下来了），
  更低版本是一层**静态渐变**兜底；真机顺带看：系统返回手势是**回主页而不是退出**、整页跟着手指滑出、
  Logo 区滚动淡出、深色下关于页底色不再发白。
- **0.11.4 的真机验收**：检查历史每一条右侧写「点击查看详情」（不再写结论，结论在详情弹窗里）；
  主页结论卡标题是「高危模块 / 中危模块 / 低危模块」（一档发现都没有时也写「低危模块」），
  那一行计数在零发现时写「未发现风险项」；底色与图标仍按最高风险走红 / 黄 / 绿。
- **0.11.1 的真机验收**：结论卡三行（大字等级 / 计数 / 模块名）不重叠，长模块名走省略号不进图标；
  检查历史点条目，详情是「发现 1/12 / 等级: / 规则: / 文件: / 说明:」逐行日志。
- **0.11.0 的真机验收**：主页应只剩三张卡；结论卡按风险变色（红 / 黄 / 绿）、右下大图标裁得好看；
  检查历史条目详情里「【发现明细】」逐条与「【提示】」都在。
- **预测性返回开关**：设置页应有四行开关（模糊 / 液态玻璃 / 预测性返回手势 / 检查更新）；关掉「预测性返回手势」后
  从关于页返回仍是回上一页，只是页面不再跟手滑出；开着时在 Android 13/14 也应该跟手（manifest 已补平台 opt-in）。
- 真机看三页观感：英雄卡实际高度与溢出图标的裁切、底栏胶囊拖动切页的手感、MiuiX 行组件在卡片里的间距。
- 给 zip 条目数加上限（防内存被打爆）。
- 用真实模块样本压误报（现在的夹具是自造的，覆盖面有限）。
- 磨砂玻璃 / 液态玻璃已落地（2026-09-29）：真机验收要看**设备门槛**——Android 12 及以下什么都看不到
  （磨砂要 API 31 的 RenderEffect、液态玻璃折射要 API 33 的 AGSL），Android 13/14 有磨砂、15+ 全都有；
  再看顶栏毛玻璃在滚动时是否自然、底栏胶囊的折射与倾斜高光是否过度。

## 已落地（倒序；细节都在 work 包里）
- 2026-09-29（第七轮）：**检查历史每条右侧写「点击查看详情」** —— 结论从卡片右侧挪进详情弹窗的 fields。
  版本 0.11.4/code16，commit `488a6f2` 已推。知识 `tooling/file-edit-anchors-and-newlines.md` 补「第七次」。
- 2026-09-29（第六轮）：**零发现那行计数改成「未发现风险项」** —— 标题三档不动，避免与标题的
  「低危模块」重复打架。版本 0.11.3/code15，commit `811bca3`（本地，未推）。
- 2026-09-29（第五轮）：**结论卡标题写「xx模块」** —— 新增 `scan_result_high/medium/low` 三条文案，
  卡片标题按最高档取「高危模块 / 中危模块 / 低危模块」（一档都没有也归最低那档）。版本 0.11.2/code14，
  commit `0becacd` 已推。知识 `tooling/file-edit-anchors-and-newlines.md` 补「第六次」。
- 2026-09-29（第四轮）：**修卡片文字重叠 + 卡片标题写等级 + 历史详情改日志写法** —— 三层 `fillMaxSize`
  叠一格导致文字互相压（装饰层改用 `matchParentSize()`、内容层单列自定高）；结论卡标题按最高等级写
  「高危 / 中危 / 低危」；检查历史详情统一成 SU 日志的「key: value」逐行格式。版本 0.11.1/code13，
  commit `8ced491`。新知识 `android/compose-box-corner-layout.md`。
- 2026-09-29（第三轮）：**结果进历史、主页只剩三张卡、检测卡按风险着色** —— 主页从「检查卡 + 结论卡 +
  逐条发现卡 + 应用版本 + 提交 BUG」收成三张（检查卡 / 应用版本 / 提交 BUG）；检查卡按 `ScanState` 变脸，
  出结果整卡着色（高红 / 中黄 / 其余绿）、版式照上游「工作中」卡；发现明细改到检查历史的条目详情里看。
  版本 0.11.0/code12，commit `b7d7bf5`（**待推送**）。细节在 work 索引与
  `android/scan-history-data-source.md` 新增的那一节。
- 2026-09-29（第二轮）：**预测性返回手势加开关** —— 设置页一行 SwitchPreference（上游文案「预测性返回手势」，
  图标 `Icons.AutoMirrored.Rounded.MenuOpen`），状态进 `DisplaySettings`；关于页 `if/else` 在
  `PredictiveBackHandler` / `BackHandler` 之间二选一（**两条不能同时挂**）；manifest 补
  `android:enableOnBackInvokedCallback`。版本 0.10.0/code11，commit `5ce94a1`。偏离上游之处
  （上游用隐藏 API 翻平台标志、重启生效）写进 `android/overlay-page-and-predictive-back.md` 的「开关」一节。
- 2026-09-29：**磨砂玻璃 + 液态玻璃 + 关于页返回手势/背景 + CI 固定签名钥匙** —— 三件事的细节都在
  work 索引里；三个新知识：`build/ci-signing-stable-key.md`、`android/miuix-blur-and-liquid-glass.md`、
  `android/overlay-page-and-predictive-back.md`（同时更正了 `compile-sdk-and-aar-metadata.md` 的 overrideLibrary
  结论与 `miuix-0.9.4.md` 的「miuix-blur 已删除」）。版本 0.9.0/code10，commit `9cc8a86`。
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

- ~~关于页背景门槛~~（2026-09-29 已定：降到 API 33，Android 13/14 也画，更低版本用静态渐变兜底）
  —— 留着只作记录，不用再问。

- 用户提「做个虚拟环境直接刷写模块看执行了哪些命令」：动态沙箱**进不了 APK**（已在本机实测假 PATH
  dry-run 并论证，见 `knowledge/detection/dry-run-sandbox.md`）。剩下的选择题：要不要在 APK 里做**静态**
  执行轨迹预览（按钩子分组列命令、零执行）？要不要另开一个 work 包做桌面/CI 侧 sandbox？
- 更新检查现在只查 `api.github.com`，**国内网络常常不通**（界面只能报「检查失败」）。要不要加镜像回退
  （比如 jsDelivr 读仓库里的 `version.json`）？
- 设置页那个「检查更新」开关现在是本页自持（`rememberSaveable`，默认开），**没有接真正的自动检查逻辑**；
  要不要接上（进主页时后台检查 + 有新版就显示 UpdateCard）？
- 本机要不要也装 NDK + gradle（约 1.6 GB）？现状「CI 编 APK、本机编 C++ 与 Kotlin 跑测试」已经够用。
- 要不要兼顾 32 位设备（armeabi-v7a）？现在只出 arm64-v8a。
