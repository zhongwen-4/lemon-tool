// 本项目自己的几个界面开关（用户 2026-09-28 要的「磨砂玻璃」与「液态玻璃」，
// 2026-09-29 又要的「预测性返回手势」，2026-10-01 又要的「检查完自动查看详情」）。
// 上游把这些开关存在 SettingsRepository（Compose DataStore）里；本项目没有那一层，
// 用最轻的 SharedPreferences：进程被杀也记得住，读的时候直接就是 Compose 状态。
package com.lemon.mrs.ui.util

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 界面上的显示开关与行为开关（顶栏 / 底栏 / 返回手势 / 检查完跳不跳详情）。
 *
 * 磨砂与液态玻璃受设备能力限制：`RenderEffect` 要 API 31、AGSL `RuntimeShader` 要 API 33，
 * 达不到时开关打开也看不出变化（上游也是这个口径，见 `ui/util/BlurExt.kt` 与 `liquid/Lens.kt` 里的门控）。
 */
class DisplaySettings(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("mrs_display", Context.MODE_PRIVATE)

    /** 磨砂玻璃：顶栏与底栏底下那层走 `textureBlur`（上游 settings_enable_blur）。 */
    var enableBlur by mutableStateOf(prefs.getBoolean(KEY_BLUR, true))
        private set

    /** 液态玻璃：底栏换成上游那枚会折射、会跟着手倾斜发亮的胶囊（上游 settings_enable_glass）。 */
    var enableFloatingBottomBar by mutableStateOf(prefs.getBoolean(KEY_GLASS, true))
        private set

    /**
     * 预测性返回手势（上游 settings_enable_predictive_back）。
     *
     * **应用级**。2026-09-30 起照上游那套接上了：Android 14+ 冷启动时由 `MrsApplication.onCreate`
     * 用隐藏 API `ApplicationInfo#setEnableOnBackInvokedCallback` 把平台标志翻成这个值；
     * 开关翻动时设置页再翻一次平台标志并 `recreate()`（上游 `ColorPaletteScreen` 同款）。
     * 所以它管的是**所有**预测性返回手势 —— 系统那套返回动画，以及 app 内每一处返回进度。
     *
     * app 内目前用到进度的只有「关于页的整页跟手滑出」：
     * 开 = `PredictiveBackHandler`（页面跟手往右滑），关 = `BackHandler`（照常回上一页，只是页面不动画）。
     * manifest 的 `android:enableOnBackInvokedCallback` 只是出厂默认，运行时会按本开关被翻掉。
     *
     * 默认**开**：上游默认是关，但本项目 0.9.0 起关于页的返回一直是跟手的，
     * 默认改成关等于把已经看到的效果悄悄关掉。
     */
    var enablePredictiveBack by mutableStateOf(prefs.getBoolean(KEY_PREDICTIVE_BACK, true))
        private set

    /**
     * 检查完自动打开这条记录的详情页（用户 2026-10-01 要的开关）。
     *
     * 开：扫描成功、写进检查历史之后，主壳直接把详情**整页**盖上来（底下那页不动，
     * 返回就回到原来那页）。关：停在主页看结论卡，详情自己去「检查历史」里点。
     * 默认**开** —— 用户先要的就是这个跳转，开关是留着给人关掉的。
     */
    var enableAutoOpenDetail by mutableStateOf(prefs.getBoolean(KEY_AUTO_OPEN_DETAIL, true))
        private set

    /**
     * 悬浮底栏自己那层要不要模糊 —— 上游是独立的 `enableFloatingBottomBarBlur`；
     * 本项目只有两个开关，就跟着「磨砂玻璃」走：模糊关掉时胶囊只剩折射与高光。
     */
    val enableFloatingBottomBarBlur: Boolean get() = enableBlur

    fun updateEnableBlur(value: Boolean) {
        enableBlur = value
        prefs.edit().putBoolean(KEY_BLUR, value).apply()
    }

    fun updateEnableFloatingBottomBar(value: Boolean) {
        enableFloatingBottomBar = value
        prefs.edit().putBoolean(KEY_GLASS, value).apply()
    }

    fun updateEnablePredictiveBack(value: Boolean) {
        enablePredictiveBack = value
        prefs.edit().putBoolean(KEY_PREDICTIVE_BACK, value).apply()
    }

    fun updateEnableAutoOpenDetail(value: Boolean) {
        enableAutoOpenDetail = value
        prefs.edit().putBoolean(KEY_AUTO_OPEN_DETAIL, value).apply()
    }

    private companion object {
        const val KEY_BLUR = "enable_blur"
        const val KEY_GLASS = "enable_glass"
        const val KEY_PREDICTIVE_BACK = "enable_predictive_back"
        const val KEY_AUTO_OPEN_DETAIL = "enable_auto_open_detail"
    }
}