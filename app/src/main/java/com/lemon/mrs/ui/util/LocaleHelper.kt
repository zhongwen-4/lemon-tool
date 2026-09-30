// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/util/LocaleHelper.kt
// 改动清单：docs/sukisu-port-changes.md#localehelper
package com.lemon.mrs.ui.util

object LocaleHelper {
    /** 「跟随系统」在语言标签列表里用空串表示。 */
    const val SYSTEM = ""

    val SUPPORTED_TAGS: List<String> = emptyList()

    fun displayName(tag: String): String = tag
}