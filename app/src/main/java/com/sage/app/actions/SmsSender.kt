package com.sage.app.actions

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

class SmsSender(private val context: Context) {
    /** Success only when every message part was reported as sent by the platform. */
    suspend fun send(number: String, body: String): Result<Unit> {
        val outcome = withTimeoutOrNull(30_000L) { sendInternal(number, body) }
        return outcome ?: Result.failure(IllegalStateException("I couldn't confirm that the message was sent."))
    }

    private suspend fun sendInternal(number: String, body: String): Result<Unit> = suspendCancellableCoroutine { cont ->
        val manager: SmsManager = if (Build.VERSION.SDK_INT >= 31) context.getSystemService(SmsManager::class.java)
        else @Suppress("DEPRECATION") SmsManager.getDefault()
        val parts = manager.divideMessage(body)
        val action = "com.sage.app.SMS_SENT_" + UUID.randomUUID()
        val remaining = AtomicInteger(parts.size)
        var failure: String? = null
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                if (resultCode != Activity.RESULT_OK && failure == null) failure = "The phone reported an SMS error (code $resultCode)."
                if (remaining.decrementAndGet() == 0) {
                    runCatching { context.unregisterReceiver(this) }
                    if (cont.isActive) cont.resume(failure?.let { Result.failure(IllegalStateException(it)) } ?: Result.success(Unit))
                }
            }
        }
        ContextCompat.registerReceiver(context, receiver, IntentFilter(action), ContextCompat.RECEIVER_NOT_EXPORTED)
        cont.invokeOnCancellation { runCatching { context.unregisterReceiver(receiver) } }
        try {
            val sent = ArrayList<PendingIntent>(parts.size)
            parts.indices.forEach { i ->
                sent += PendingIntent.getBroadcast(context, i, Intent(action).setPackage(context.packageName), PendingIntent.FLAG_IMMUTABLE)
            }
            if (parts.size == 1) manager.sendTextMessage(number, null, body, sent[0], null)
            else manager.sendMultipartTextMessage(number, null, parts, sent, null)
        } catch (t: Throwable) {
            runCatching { context.unregisterReceiver(receiver) }
            if (cont.isActive) cont.resume(Result.failure(IllegalStateException(t.message ?: "Could not send the SMS.")))
        }
    }
}
