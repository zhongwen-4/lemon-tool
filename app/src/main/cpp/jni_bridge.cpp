#include <jni.h>

#include <string>

#include "mrs/core.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_lemon_mrs_MainActivity_nativeScanJson(JNIEnv* env, jobject thiz, jstring path) {
    (void)thiz;
    if (path == nullptr) return env->NewStringUTF("{\"error\":\"路径为空\"}");
    const char* raw = env->GetStringUTFChars(path, nullptr);
    std::string target = raw ? raw : "";
    if (raw) env->ReleaseStringUTFChars(path, raw);

    std::string error;
    mrs::Report report = mrs::scan_zip(target, &error);
    if (!error.empty()) {
        return env->NewStringUTF(("{\"error\":\"" + error + "\"}").c_str());
    }
    return env->NewStringUTF(mrs::to_json(report).c_str());
}
