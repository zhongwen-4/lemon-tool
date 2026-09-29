# 预测性返回手势的开关要做成「应用级」：反射翻 ApplicationInfo 的标志 + recreate

SUMMARY: 上游 SukiSU 的「预测性返回手势」开关管的是**整个 app**：Android 14+ 冷启动
（`KernelSUApplication.onCreate`）与开关翻动时（`ColorPaletteScreen`）用 `HiddenApiBypass` 放行后反射调
隐藏 API `ApplicationInfo#setEnableOnBackInvokedCallback`，翻掉进程内那份 ApplicationInfo 的标志，
翻动时再 `recreate()`。只在某一页用 `PredictiveBackHandler` / `BackHandler` 二选一是**假开关** ——
系统那套返回动画（返回桌面时窗口跟手缩看）根本不归它管，用户 2026-09-30 报的正是这个。
本项目 0.11.6/code18 照搬：新增 `MrsApplication` + 依赖 `org.lsposed.hiddenapibypass:hiddenapibypass:6.1`。

READ WHEN: before 动设置页的「预测性返回手势」开关、要给某处返回接预测性返回，
或要在本项目里反射调隐藏 API（hidden API）时。

RECHECK WHEN: 平台把这份标志的读取位置挪走（现在读的是进程内的 ApplicationInfo）、
或者 targetSdk 抬到让 `enableOnBackInvokedCallback` 变成强制之后。

---

## 上游原文（`manager/app/src/main/java/com/sukisu/ultra/`）

`KernelSUApplication.kt`：

```kotlin
companion object {
    fun setEnableOnBackInvokedCallback(appInfo: ApplicationInfo, enable: Boolean) {
        runCatching {
            val applicationInfoClass = ApplicationInfo::class.java
            val method = applicationInfoClass.getDeclaredMethod("setEnableOnBackInvokedCallback", Boolean::class.javaPrimitiveType)
            method.isAccessible = true
            method.invoke(appInfo, enable)
        }
    }
}

override fun onCreate() {
    ...
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val enable = SettingsRepositoryImpl().enablePredictiveBack
        HiddenApiBypass.addHiddenApiExemptions("Landroid/content/pm/ApplicationInfo;->setEnableOnBackInvokedCallback")
        setEnableOnBackInvokedCallback(applicationInfo, enable)
    }
}
```

`ui/screen/colorpalette/ColorPaletteScreen.kt`（开关的回调）：

```kotlin
onSetEnablePredictiveBack = {
    viewModel.setEnablePredictiveBack(it)
    KernelSUApplication.setEnableOnBackInvokedCallback(context.applicationInfo, it)
    activity?.recreate()
},
```

几个要点：

- 上游 manifest **没有** `android:enableOnBackInvokedCallback`；等效的出厂值就是它的默认 `false`
  （`SettingsRepositoryImpl`：`prefs.getBoolean("enable_predictive_back", false)`）。
- 开关只在 API 34+ 显示（`UPSIDE_DOWN_CAKE`），更低版本平台没有这个标志。
- `addHiddenApiExemptions` 只放行这一条签名，不是 `"L"` 那种全放行。

## 本项目怎么接（0.11.6/code18）

| 位置 | 干了什么 |
| --- | --- |
| `app/build.gradle` | `implementation 'org.lsposed.hiddenapibypass:hiddenapibypass:6.1'` |
| `MrsApplication.kt`（新增） | ① companion 里那份 `setEnableOnBackInvokedCallback`（上游原样）；② `onCreate` 里 API 34+ 读 `DisplaySettings.enablePredictiveBack` 并翻标志 |
| `AndroidManifest.xml` | `<application android:name=".MrsApplication">`；`android:enableOnBackInvokedCallback="true"` 留着当出厂默认（运行时会按开关被翻掉） |
| `SettingsMiuix.kt` | 「预测性返回手势」的 `onCheckedChange`：存开关 → 翻平台标志 → `recreate()`（跟上游一样只在 API 34+ 做） |
| `DisplaySettings.kt` | doc 改成「应用级」；默认仍是 **true**（上游默认 false，本项目 0.9.0 起关于页就是跟手的，改默认等于悄悄关掉用户已经看到的效果） |
| `AboutScreen.kt` | 不动：开 = `PredictiveBackHandler`（跟手滑出）、关 = `BackHandler` |

## 坑

- **`recreate()` 会丢 `remember{}` 的界面状态**。上游不在乎（它那页的状态都在 ViewModel / saveable 里）；
  本项目 `ScannerShell` 的 `state`（扫描结果）与 `targetName` 是 `remember{}`，翻这个开关会把主页上
  「刚扫完」的结论卡清回 idle —— 结果本身已经落盘进「检查历史」，所以能接受，但真机上看起来像
  「翻个开关结果没了」，别当成新 bug。tab 下标走 `rememberSaveable`，不会跳回主页。
- **Application 类名不能被混淆**，而这一步不用自己写 keep 规则：AGP 从合并 manifest 生成
  `app/build/intermediates/aapt_proguard_file/release/processReleaseResources/aapt_rules.txt`，
  里面自动有 `-keep class com.lemon.mrs.MrsApplication { <init>(); }`。
- 校验 R8 有没有把新东西剪掉：看 `app/build/outputs/mapping/release/usage.txt`（列的是**被删掉的**）——
  本次 `MrsApplication` / `HiddenApiBypass` 只被删了 `$stable` / `Companion` / `TAG` /
  `$assertionsDisabled` 这类，类与方法都在；`LSPass` / `BuildConfig` / `R` 整个删掉是对的（没人引用）。
- **`hiddenapibypass` 的坐标只有 `.aar`**（`hiddenapibypass-6.1.aar` + `.module`），直接猜 `-6.1.jar`
  会 404，别据此判定「库不存在」。
- 反射调用包在 `runCatching` 里（上游如此）：低版本 / 平台改名时静默失败，别指望它报错。
- 新增依赖后要**先联网跑一次**让构件进 `~/.gradle/caches`，否则本机那 8 条 `--offline` 前置会 FAILED
  （`--offline` 不会去下载）。
