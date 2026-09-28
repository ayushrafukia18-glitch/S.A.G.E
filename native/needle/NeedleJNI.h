#ifndef SAGE_NEEDLE_JNI_H
#define SAGE_NEEDLE_JNI_H
#include <jni.h>
extern "C" {
JNIEXPORT jint JNICALL Java_com_sage_app_needle_NeedleBridge_nativeInit(
    JNIEnv*, jobject, jstring modelPath, jstring systemPrompt, jstring toolsJson, jstring toolIndexPath);
JNIEXPORT jstring JNICALL Java_com_sage_app_needle_NeedleBridge_nativeRoute(
    JNIEnv*, jobject, jstring requestText, jint maxNewTokens);
JNIEXPORT void JNICALL Java_com_sage_app_needle_NeedleBridge_nativeReset(JNIEnv*, jobject);
JNIEXPORT void JNICALL Java_com_sage_app_needle_NeedleBridge_nativeClose(JNIEnv*, jobject);
JNIEXPORT jstring JNICALL Java_com_sage_app_needle_NeedleBridge_nativeLastError(JNIEnv*, jobject);
}
#endif
