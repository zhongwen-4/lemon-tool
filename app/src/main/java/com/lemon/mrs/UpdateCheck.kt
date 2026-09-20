package com.lemon.mrs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val RELEASE_API = "https://api.github.com/repos/zhongwen-4/lemon-tool/releases/latest"
private const val TIMEOUT_MS = 8000

sealed interface UpdateResult {
    data class Newer(val version: String, val url: String) : UpdateResult
    data object Current : UpdateResult
    data class Failed(val message: String) : UpdateResult
}

/** 取最新 Release 的 tag 与已安装版本比对。除了这一次请求，扫描本身全程离线。 */
suspend fun checkUpdate(installed: String): UpdateResult = withContext(Dispatchers.IO) {
    runCatching {
        val connection = (URL(RELEASE_API).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "lemon-mrs")
        }
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("HTTP ${connection.responseCode}")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val tag = json.optString("tag_name")
            if (tag.isBlank()) throw IllegalStateException("响应里没有 tag_name")
            val latest = tag.removePrefix("v")
            if (isNewer(latest, installed)) {
                UpdateResult.Newer(latest, json.optString("html_url"))
            } else {
                UpdateResult.Current
            }
        } finally {
            connection.disconnect()
        }
    }.getOrElse { error -> UpdateResult.Failed(error.message ?: error.javaClass.simpleName) }
}

/** 逐段比数字：0.2.0 > 0.1.0；非数字段当 0。 */
private fun isNewer(latest: String, installed: String): Boolean {
    val left = latest.split('.', '-', '+')
    val right = installed.split('.', '-', '+')
    for (index in 0 until maxOf(left.size, right.size)) {
        val a = left.getOrNull(index)?.toIntOrNull() ?: 0
        val b = right.getOrNull(index)?.toIntOrNull() ?: 0
        if (a != b) return a > b
    }
    return false
}
