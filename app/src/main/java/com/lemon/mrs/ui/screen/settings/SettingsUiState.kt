// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/screen/settings/SettingsUiState.kt
// 改动（改动日期：2026-09-28）：
//   ① 上游的 colorStyle / colorSpec 用的是 com.materialkolor 的 PaletteStyle / ColorSpec 枚举名，
//      本项目没引 materialkolor，这里改成同名字符串（PaletteStyle.TonalSpot.name / ColorSpec.SpecVersion.SPEC_2025.name）。
//   ② SettingsScreenActions 的每个回调都补了空实现，这样设置页可以先用 SettingsUiState() +
//      SettingsScreenActions() 建成「组件骨架、数据空着」的样子。
package com.lemon.mrs.ui.screen.settings

import androidx.compose.runtime.Immutable
import com.lemon.mrs.ui.UiMode

@Immutable
data class SettingsUiState(
    val uiMode: String = UiMode.DEFAULT_VALUE,
    val appLanguage: String = "",
    val checkUpdate: Boolean = true,
    val checkModuleUpdate: Boolean = true,
    val alternativeIcon: Boolean = false,
    val themeMode: Int = 0,
    val miuixMonet: Boolean = false,
    val keyColor: Int = 0,
    val colorStyle: String = "TonalSpot",
    val colorSpec: String = "SPEC_2025",
    val enablePredictiveBack: Boolean = false,
    val enableSwipeDismiss: Boolean = true,
    val pagerInterceptionMode: Int = 1,
    val enableBlur: Boolean = true,
    val enableFloatingBottomBar: Boolean = false,
    val enableFloatingBottomBarBlur: Boolean = false,
    val enableNavigationBadge: Boolean = true,
    val pageScale: Float = 1.0f,
    val moduleDescriptionMaxLines: Int = 4,
    val enableWebDebugging: Boolean = false,
    val showFullStatus: Boolean = true,

    // Su Compat
    val suCompatStatus: String = "",
    val suCompatMode: Int = 0, // 0: enable default, 1: disable until reboot, 2: disable always
    val isSuEnabled: Boolean = false,

    // Kernel Umount
    val kernelUmountStatus: String = "",
    val isKernelUmountEnabled: Boolean = false,

    // SELinux Hide
    val selinuxHideStatus: String = "",
    val isSelinuxHideEnabled: Boolean = false,

    // SU Log
    val sulogStatus: String = "",
    val isSulogEnabled: Boolean = false,

    // Umount Modules
    val isDefaultUmountModules: Boolean = false,

    // ADB Root
    val adbRootStatus: String = "",
    val isAdbRootEnabled: Boolean = false,

    val isLkmMode: Boolean = false,
    val isLateLoadMode: Boolean = false,

    // Auto Jailbreak
    val autoJailbreak: Boolean = false,

    // Soft Reboot
    val useSoftReboot: Boolean = false,
)

@Immutable
data class SettingsScreenActions(
    val onSetCheckUpdate: (Boolean) -> Unit = {},
    val onSetCheckModuleUpdate: (Boolean) -> Unit = {},
    val onOpenTheme: () -> Unit = {},
    val onSetUiModeIndex: (Int) -> Unit = {},
    val onOpenProfileTemplate: () -> Unit = {},
    val onSetLanguage: (String) -> Unit = {},
    val onSetSuCompatMode: (Int) -> Unit = {},
    val onSetKernelUmountEnabled: (Boolean) -> Unit = {},
    val onSetSelinuxHideEnabled: (Boolean) -> Unit = {},
    val onSetSulogEnabled: (Boolean) -> Unit = {},
    val onSetAdbRootEnabled: (Boolean) -> Unit = {},
    val onSetDefaultUmountModules: (Boolean) -> Unit = {},
    val onSetEnableWebDebugging: (Boolean) -> Unit = {},
    val onSetAutoJailbreak: (Boolean) -> Unit = {},
    val onSetUseSoftReboot: (Boolean) -> Unit = {},
    val onOpenAbout: () -> Unit = {},
    val onSetAlternativeIcon: (Boolean) -> Unit = {},
    val onOpenTools: () -> Unit = {},
    val onOpenKpm: () -> Unit = {},
    val onOpenSusfsConfig: () -> Unit = {},
)