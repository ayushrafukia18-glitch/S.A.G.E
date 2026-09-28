package com.sage.app.actions

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.sage.app.MainActivity
import com.sage.app.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val text = intent.getStringExtra(EXTRA_TEXT).orEmpty()
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "SAGE reminders", NotificationManager.IMPORTANCE_HIGH))
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("SAGE reminder")
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify(id.hashCode(), notification)
        if (id >= 0) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try { AppDatabase.getInstance(context).reminderDao().setStatus(id, "completed") } finally { pendingResult.finish() }
            }
        }
    }
    companion object { const val CHANNEL = "sage_reminders"; const val EXTRA_ID = "id"; const val EXTRA_TEXT = "text" }
}
