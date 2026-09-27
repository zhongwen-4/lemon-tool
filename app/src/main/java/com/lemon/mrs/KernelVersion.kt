// 对应 SukiSU Ultra（GPL-3.0）的 com.sukisu.ultra.KernelVersion。
// 改动：上游那个类带完整的版本解析与比较；主页骨架目前只用到 isGKI，所以先留最小占位，
//       数据先空着，往后接真实内核信息时再补齐（改动日期：2026-09-28）。
package com.lemon.mrs

class KernelVersion {
    fun isGKI(): Boolean = false
}