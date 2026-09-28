// 本项目自己的两个显示开关（用户 2026-09-28 要的「磨砂玻璃」与「液态玻璃」）。
// 上游把这两个开关存在 SettingsRepository（Compose DataStore）里；本项目没有那一层，
// 用最轻的 SharedPreferences：进程被杀也记得住，读的时候直接就是 Compose 状态。
package com.lemon.mrs.ui.util

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 顶栏 / 底栏的显示开关。
 *
 * 两个开关都受设备能力限制：`RenderEffect` 要 API 31、AGSL `RuntimeShader` 要 API 33，
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

    private companion object {
        const val KEY_BLUR = "enable_blur"
        const val KEY_GLASS = "enable_glass"
    }
}