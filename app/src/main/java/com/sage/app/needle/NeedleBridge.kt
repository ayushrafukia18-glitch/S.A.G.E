package com.sage.app.needle

internal object NeedleBridge {
    init {
        System.loadLibrary("sage_needle_jni")
    }

    external fun nativeInit(
        modelPath: String,
        systemPrompt: String,
        toolsJson: String,
        toolIndexPath: String?
    ): Int

    external fun nativeRoute(request: String, maxNewTokens: Int): String
    external fun nativeReset()
    external fun nativeClose()
    external fun nativeLastError(): String?
}
