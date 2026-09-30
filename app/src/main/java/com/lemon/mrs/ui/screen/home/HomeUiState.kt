// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/screen/home/HomeUiState.kt
// 改动清单：docs/sukisu-port-changes.md#homeuistate
package com.lemon.mrs.ui.screen.home

import androidx.compose.runtime.Immutable
import com.lemon.mrs.KernelVersion
import com.lemon.mrs.ui.util.module.LatestVersionInfo

@Immutable
data class HomeUiState(
    val kernelVersion: KernelVersion = KernelVersion(),
    val ksuVersion: Int? = null,
    val managerUAPIVersion: Int = 0,
    val kernelUAPIVersion: Int? = null,
    val lkmMode: Boolean? = null,
    val isLkmBundled: Boolean = false,
    val isManager: Boolean = false,
    val isManagerPrBuild: Boolean = false,
    val isKernelPrBuild: Boolean = false,
    val requiresNewKernel: Boolean = false,
    val requiresNewManager: Boolean = false,
    val isRootAvailable: Boolean = false,
    val isSafeMode: Boolean = false,
    val isLateLoadMode: Boolean = false,
    val checkUpdateEnabled: Boolean = false,
    val latestVersionInfo: LatestVersionInfo = LatestVersionInfo(),
    val currentManagerVersionCode: Long = 0L,
    val systemInfo: SystemInfo = SystemInfo(),
    val showFullStatus: Boolean = true,
) {
    val isSELinuxPermissive: Boolean
        get() = systemInfo.selinuxStatus == "Permissive"

    val showGkiWarning: Boolean
        get() = ksuVersion != null && lkmMode == false

    val showLkmUpdate: Boolean
        get() = isManager &&
                lkmMode == true &&
                isLkmBundled &&
                ksuVersion?.toLong() != currentManagerVersionCode &&
                !requiresNewKernel &&
                !requiresNewManager

    val showCustomLkmBadge: Boolean
        get() = lkmMode == true && !isLkmBundled

    val showRootWarning: Boolean
        get() = ksuVersion != null && !isRootAvailable

    val showManagerPrBuildWarning: Boolean
        get() = isManager && isManagerPrBuild

    val showKernelPrBuildWarning: Boolean
        get() = isManager && !isManagerPrBuild && isKernelPrBuild

    val hasUpdate: Boolean
        get() = latestVersionInfo.versionCode > currentManagerVersionCode
}

@Immutable
data class HomeActions(
    val onInstallClick: () -> Unit = {},
    val onOpenUrl: (String) -> Unit = {},
    val onJailbreakClick: () -> Unit = {},
)
