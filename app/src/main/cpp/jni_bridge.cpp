#include <jni.h>

#include <string>

#include "mrs/core.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_lemon_mrs_MainActivity_nativeScan(JNIEnv* env, jclass clazz, jstring path) {
    if (path == nullptr) return env->NewStringUTF("扫描失败：路径为空");
    const char* raw = env->GetStringUTFChars(path, nullptr);
    std::string target = raw ? raw : "";
    if (raw) env->ReleaseStringUTFChars(path, raw);

    std::string error;
    mrs::Report report = mrs::scan_zip(target, &error);
    std::string text = error.empty() ? mrs::to_text(report) : "扫描失败：" + error;
    return env->NewStringUTF(text.c_str());
}
