package com.lemon.mrs

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        System.loadLibrary("mrs_jni")
        setContent {
            ScannerScreen { path -> nativeScanJson(path) }
        }
    }

    private external fun nativeScanJson(path: String): String

    companion object {
        internal const val CACHE_NAME = "picked_module.zip"

        fun copyToCache(context: Context, uri: Uri): String {
            val target = File(context.cacheDir, CACHE_NAME)
            val source = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("无法打开所选文件")
            source.use { input ->
                FileOutputStream(target).use { sink -> input.copyTo(sink, 65536) }
            }
            return target.absolutePath
        }

        /** 缓存里那个临时 zip 的大小；设置页用它显示「可清理」多少。 */
        fun cachedModuleBytes(context: Context): Long =
            File(context.cacheDir, CACHE_NAME).takeIf { it.exists() }?.length() ?: 0L

        fun clearCachedModule(context: Context) {
            File(context.cacheDir, CACHE_NAME).takeIf { it.exists() }?.delete()
        }
    }
}
