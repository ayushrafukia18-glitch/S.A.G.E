#include "NeedleJNI.h"
#include "needle.h"
#include <android/log.h>
#include <fstream>
#include <mutex>
#include <string>
#include <vector>

#define LOG_TAG "SageNeedleJNI"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {
std::mutex g_mutex;
bool g_loaded = false;
bool g_initialized = false;

std::string jstringToString(JNIEnv* env, jstring value) {
    if (!value) return {};
    const char* chars = env->GetStringUTFChars(value, nullptr);
    if (!chars) return {};
    std::string result(chars);
    env->ReleaseStringUTFChars(value, chars);
    return result;
}

jstring stringToJString(JNIEnv* env, const std::string& value) {
    return env->NewStringUTF(value.c_str());
}

std::string jsonEscape(const std::string& in) {
    std::string out;
    for (unsigned char c : in) {
        switch (c) {
            case '"': out += "\\\""; break;
            case '\\': out += "\\\\"; break;
            case '\n': out += "\\n"; break;
            case '\r': out += "\\r"; break;
            case '\t': out += "\\t"; break;
            default: if (c < 0x20) out += ' '; else out += static_cast<char>(c);
        }
    }
    return out;
}

std::string lastError() {
    const char* e = needle_last_error();
    return e ? std::string(e) : std::string();
}

bool loadWeights(const std::string& path) {
    std::ifstream file(path, std::ios::binary);
    if (!file) return false;
    file.seekg(0, std::ios::end);
    const auto size = file.tellg();
    if (size <= 0) return false;
    file.seekg(0, std::ios::beg);
    std::vector<unsigned char> bytes(static_cast<size_t>(size));
    file.read(reinterpret_cast<char*>(bytes.data()), size);
    if (!file) return false;
    return needle_load(bytes.data(), static_cast<unsigned long long>(bytes.size())) >= 0;
}
}

extern "C" {

JNIEXPORT jint JNICALL
Java_com_sage_app_needle_NeedleBridge_nativeInit(JNIEnv* env, jobject,
                                                  jstring modelPath,
                                                  jstring systemPrompt,
                                                  jstring toolsJson,
                                                  jstring toolIndexPath) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_initialized) return 0;
    if (!g_loaded) {
        if (!loadWeights(jstringToString(env, modelPath))) {
            LOGE("needle_load failed: %s", lastError().c_str());
            return -1;
        }
        g_loaded = true;
    }
    const std::string system = jstringToString(env, systemPrompt);
    const std::string tools = jstringToString(env, toolsJson);
    const std::string index = jstringToString(env, toolIndexPath);
    int rc = needle_init(system.c_str(), tools.c_str(), index.empty() ? nullptr : index.c_str());
    if (rc < 0) {
        LOGE("needle_init failed: %s", lastError().c_str());
        return rc;
    }
    g_initialized = true;
    return rc;
}

JNIEXPORT jstring JNICALL
Java_com_sage_app_needle_NeedleBridge_nativeRoute(JNIEnv* env, jobject,
                                                   jstring requestText,
                                                   jint maxNewTokens) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (!g_initialized) return stringToJString(env, "{\"type\":\"respond\",\"success\":false,\"error\":\"Needle not initialized\",\"function_calls\":[]}");
    constexpr int kCapacity = 65536;
    std::vector<char> output(kCapacity, 0);
    const std::string request = jstringToString(env, requestText);
    const int rc = needle_complete(request.c_str(), maxNewTokens, output.data(), kCapacity - 1);
    if (rc < 0) {
        const std::string e = lastError();
        return stringToJString(env, std::string("{\"type\":\"respond\",\"success\":false,\"error\":\"") + jsonEscape(e) + "\",\"function_calls\":[]}");
    }
    return stringToJString(env, std::string(output.data()));
}

JNIEXPORT void JNICALL
Java_com_sage_app_needle_NeedleBridge_nativeReset(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_initialized) needle_reset();
}

JNIEXPORT void JNICALL
Java_com_sage_app_needle_NeedleBridge_nativeClose(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_initialized) needle_reset();
    g_initialized = false;
    g_loaded = false;
}

JNIEXPORT jstring JNICALL
Java_com_sage_app_needle_NeedleBridge_nativeLastError(JNIEnv* env, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    return stringToJString(env, lastError());
}

}
