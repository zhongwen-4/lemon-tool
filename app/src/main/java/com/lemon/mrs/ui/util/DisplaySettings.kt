// 本项目自己的三个显示开关（用户 2026-09-28 要的「磨砂玻璃」与「液态玻璃」，
// 2026-09-29 又要的「预测性返回手势」）。
// 上游把这三个开关存在 SettingsRepository（Compose DataStore）里；本项目没有那一层，
// 用最轻的 SharedPreferences：进程被杀也记得住，读的时候直接就是 Compose 状态。
package com.lemon.mrs.ui.util

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 界面上的显示开关（顶栏 / 底栏 / 返回手势）。
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
     * 上游是**应用级**的：Android 14+ 启动时用隐藏 API `ApplicationInfo#setEnableOnBackInvokedCallback`
     * 翻平台的预测性返回标志（`KernelSUApplication.onCreate`）。本项目没有那层隐藏 API 依赖，
     * 预测性返回也只有「关于页的整页跟手滑出」这一处，所以这个开关直接管那一处：
     * 开 = `PredictiveBackHandler`（页面跟手往右滑），关 = `BackHandler`（照常回上一页，只是页面不动画）。
     * 平台的 opt-in 走 manifest 的 `android:enableOnBackInvokedCallback`（本开关不参与）。
     *
     * 默认**开**：上游默认是关，但本项目 0.9.0 起关于页的返回一直是跟手的，
     * 默认改成关等于把已经看到的效果悄悄关掉。
     */
    var enablePredictiveBack by mutableStateOf(prefs.getBoolean(KEY_PREDICTIVE_BACK, true))
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

    private companion object {
        const val KEY_BLUR = "enable_blur"
        const val KEY_GLASS = "enable_glass"
        const val KEY_PREDICTIVE_BACK = "enable_predictive_back"
    }
}