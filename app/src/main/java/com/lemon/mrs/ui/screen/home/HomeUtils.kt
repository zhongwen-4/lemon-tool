// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/screen/home/HomeUtils.kt
// 改动清单：docs/sukisu-port-changes.md#homeutils
package com.lemon.mrs.ui.screen.home

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.pm.PackageInfoCompat

data class ManagerVersion(
    val versionName: String = "",
    val versionCode: Long = 0L,
)

data class SystemInfo(
    val kernelVersion: String = "",
    val managerVersion: String = "",
    val deviceModel: String = "",
    val kernelFullVersion: String? = null,
    val fingerprint: String = "",
    val selinuxStatus: String = "",
    val seccompStatus: Int = 0,
    val zygiskImplementation: String? = null,
)

fun getManagerVersion(context: Context): ManagerVersion = runCatching {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    ManagerVersion(
        versionName = packageInfo.versionName.orEmpty(),
        versionCode = PackageInfoCompat.getLongVersionCode(packageInfo),
    )
}.getOrDefault(ManagerVersion())
// 上游同文件里这几个是内核取数（SuSFS / hook 类型）。数据先空着：留同签名的空实现，
// 让 InfoCard 的调用点与上游保持一致，往后接真实取数即可。
enum class SusfsStatus { Idle, Supported, Unsupported, Error }

data class SusfsInfoState(
    val status: SusfsStatus = SusfsStatus.Idle,
    val detail: String = "",
)

@Composable
@Suppress("UNUSED_PARAMETER")
fun rememberSusfsInfo(
    manualHookLabel: String,
    inlineHookLabel: String,
): SusfsInfoState = remember { SusfsInfoState() }

@Composable
@Suppress("UNUSED_PARAMETER")
fun rememberHookTypeLabel(
    manualHookText: String,
    inlineHookText: String,
    tracepointHookText: String,
    unknownHookText: String,
): String? = null