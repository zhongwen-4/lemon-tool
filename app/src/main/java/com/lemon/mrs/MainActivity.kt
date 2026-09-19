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
        private const val CACHE_NAME = "picked_module.zip"

        fun copyToCache(context: Context, uri: Uri): String {
            val target = File(context.cacheDir, CACHE_NAME)
            val source = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("无法打开所选文件")
            source.use { input ->
                FileOutputStream(target).use { sink -> input.copyTo(sink, 65536) }
            }
            return target.absolutePath
        }
    }
}
