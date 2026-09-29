// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/KernelSUApplication.kt
// 改动（改动日期：2026-09-30）：
//   ① 只搬「预测性返回的平台标志」这一件事。上游这个类还管 OkHttp 缓存、SuperUserViewModel 预热、
//      webroot、TMPDIR 等，本项目一样都不需要（没有内核那一侧的东西）。
//   ② 上游在 onCreate 里按存下来的开关翻标志；本项目同一个口径，开关值从 DisplaySettings 读。
//   ③ 上游的类名叫 KernelSUApplication，本项目叫 MrsApplication。
package com.lemon.mrs

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.Build
import com.lemon.mrs.ui.util.DisplaySettings
import org.lsposed.hiddenapibypass.HiddenApiBypass

/**
 * 本 App 的 Application：目前只做一件事 —— 冷启动时按「预测性返回手势」开关翻平台的预测性返回标志。
 *
 * 为什么要 Application：这个标志是**进程级**的，必须在任何 Activity 起来之前就设好；
 * 上游也是这么放的。
 */
class MrsApplication : Application() {

    companion object {
        /**
         * 翻「这个 app 支不支持预测性返回」的平台标志（上游 `KernelSUApplication.setEnableOnBackInvokedCallback` 原样搬来）。
         *
         * `ApplicationInfo#setEnableOnBackInvokedCallback` 是隐藏 API，Android 9 起反射调不到，
         * 所以调用前要先让 [HiddenApiBypass] 放行这一条签名。它管的是**整个 app**：
         * 关掉之后系统的预测性返回动画与 app 内收到的返回进度一起没有。
         */
        fun setEnableOnBackInvokedCallback(appInfo: ApplicationInfo, enable: Boolean) {
            runCatching {
                val method = ApplicationInfo::class.java
                    .getDeclaredMethod("setEnableOnBackInvokedCallback", Boolean::class.javaPrimitiveType)
                method.isAccessible = true
                method.invoke(appInfo, enable)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        // 上游只在 Android 14+ 做这件事：更低版本的平台没有这个标志，反射也没这个方法。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val enable = DisplaySettings(this).enablePredictiveBack
            HiddenApiBypass.addHiddenApiExemptions("Landroid/content/pm/ApplicationInfo;->setEnableOnBackInvokedCallback")
            setEnableOnBackInvokedCallback(applicationInfo, enable)
        }
    }
}
