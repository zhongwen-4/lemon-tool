// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/util/module/LatestVersionInfo.kt
// 改动：只改包名（改动日期：2026-09-28）
package com.lemon.mrs.ui.util.module

data class LatestVersionInfo(
    val versionCode: Long = 0L,
    val downloadUrl: String = "",
    val changelog: String = ""
)
