package com.sage.app.actions

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

class ReminderScheduler(private val context: Context) {
    private fun pending(id: Long, text: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_ID, id)
            putExtra(ReminderReceiver.EXTRA_TEXT, text)
        }
        return PendingIntent.getBroadcast(context, id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    /** Returns true when scheduled as an exact alarm, false when only an inexact alarm was allowed. */
    fun schedule(id: Long, text: String, atMillis: Long): Boolean {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val exact = Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms()
        if (exact) alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending(id, text))
        else alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending(id, text))
        return exact
    }

    fun cancel(id: Long, text: String) {
        context.getSystemService(AlarmManager::class.java).cancel(pending(id, text))
    }
}
