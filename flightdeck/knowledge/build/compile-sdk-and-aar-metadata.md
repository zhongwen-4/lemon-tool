# compileSdk / targetSdk 与 AAR 元数据这两道硬门（升 Android 库之后必查）

SUMMARY: 升 Compose / MiuiX / androidx 之后有**两道只有「出包」才会撞上的硬门**：
① AAR 元数据里的 `minCompileSdk` —— 库要求 `compileSdk` 不得低于某个版本，撞上就在
`check<Debug,Release>AarMetadata` 失败；② 库 manifest 里的 `minSdkVersion` —— 比本项目 minSdk 高，
就在**清单合并**（`process<Debug,Release>MainManifest`）失败。
2026-09-27 本项目两条都撞了：MiuiX 0.9.4 全家桶要求 **compileSdk ≥ 37**；`miuix-blur-android:0.9.4`
的 manifest 硬要求 **minSdk 33**，与本项目 minSdk 24 直接冲突，最后是**删掉这个坐标**收场。
**`compileSdk` 只决定「编译时能看到哪个 android.jar 的 API」，不 opt-in 任何运行时行为；改行为的是
`targetSdk`** —— 所以升 compileSdk 是安全的，targetSdk 可以原地不动。
READ WHEN: when 升级 Compose / MiuiX / androidx 依赖，或 CI 报 `checkReleaseAarMetadata FAILED`、
`requires ... to compile against version N or later`、`uses-sdk:minSdkVersion ... cannot be smaller than
version ... declared in library` 时。
RECHECK WHEN: 再升 MiuiX / compose-bom，或 AGP 换大版本之后（`compileSdkMinor` 的写法可能变）。

---

## 症状原文（CI run `36281282791` 的 apk job，commit `5d87549`）

```
> Task :app:checkReleaseAarMetadata FAILED
> A failure occurred while executing com.android.build.gradle.internal.tasks.CheckAarMetadataWorkAction
   > 20 issues were found when checking AAR metadata:
      1. Dependency 'top.yukonga.miuix.kmp:miuix-preference-android:0.9.4' requires libraries and
         applications that depend on it to compile against version 37 or later of the Android APIs.
         :app is currently compiled against android-36.
         Recommended action: Update this project to use a newer compileSdk of at least 37, for example 37.2.
```

20 条全是同一类：MiuiX 0.9.4 的 5 个坐标 + `compose 1.12.1` / `lifecycle 2.11.0` /
`material3-window-size-class 1.5.0-alpha22` / `materialkolor 5.0.1` 都写了 `minCompileSdk=37`。

按提示升完 compileSdk，本机再跑 `processReleaseMainManifest`，**第二道门立刻接着炸**：

```
Manifest merger failed : uses-sdk:minSdkVersion 24 cannot be smaller than version 33 declared in
library [top.yukonga.miuix.kmp:miuix-blur-android:0.9.4] as the library might be using APIs not
available in 24
```

（这条 CI 上还没跑到，是本地先跑出来的：升 MiuiX 到 0.9.4 的 commit `814ab5b` 之后，
第一次跑 apk job 就停在前面那道 AAR 门了。）

## 修法（2026-09-27 落地，commit `6af075b`）

- `app/build.gradle`：`compileSdk 36` → `compileSdk 37` + **`compileSdkMinor 2`**；`targetSdk` 保持 36 不动。
- 删掉 `implementation 'top.yukonga.miuix.kmp:miuix-blur-android:0.9.4'`
  （代码里一处都没用它 —— 移植 SukiSU 界面时毛玻璃那一路已经砍了，见 `android/sukisu-ui-port.md`）。
- CI（`.github/workflows/android.yml`）：`"platforms;android-36" "build-tools;36.0.0"`
  → `"platforms;android-37.2" "build-tools;37.0.0"`。
- 本机 SDK 补装：`platforms;android-37.2` + `build-tools;37.0.0`（要联网，`--offline` 装不了；
  许可证早就接受过，直接 `--install` 即可）。

## 要点

- **SDK 平台从 37 起按小版本发布**：SDK 仓库里只有 `platforms;android-37.0 / 37.1 / 37.2`（还有 beta），
  **没有裸的 `android-37`**。AGP 9.4.1 的 DSL 是 `compileSdk 37` 配 `compileSdkMinor 2`，
  Gradle 才会去用 `platforms;android-37.2`；minor 写错就直接报「找不到平台」。
- `compileSdk` 与 `targetSdk` 语义不同：前者是编译期 API 面，**不 opt-in 新行为**；权限、edge-to-edge、
  后台限制这些运行时行为归后者管。日志推荐的是 compileSdk，别顺手把 targetSdk 一起提。
- 库的 minSdk 门槛**别用 `tools:overrideLibrary` 硬过** —— 库真调了高版本 API 时会在低版本设备上崩。
  正解是删依赖，或抬高本项目 minSdk（会丢老设备）。
- 本地提前查 AAR 门槛，省一轮 CI：`7z e -so <aar> META-INF/com/android/build/gradle/aar-metadata.properties`
  （`7z` 在 `D:\7z\7z.exe`）。Gradle 缓存里的解包产物在
  `C:\Users\admin\.gradle\caches\9.7.1\transforms\*\transformed\<名字>\`。
- 本机必跑这两条（`compile*Kotlin` **不在**它们的依赖链上，所以覆盖不到；详见
  `build/android-toolchain.md` 的「本机验证能走到哪一步」）：

  ```powershell
  .\gradlew.bat :app:checkDebugAarMetadata :app:checkReleaseAarMetadata `
      :app:processDebugMainManifest :app:processReleaseMainManifest --offline --console=plain
  ```
