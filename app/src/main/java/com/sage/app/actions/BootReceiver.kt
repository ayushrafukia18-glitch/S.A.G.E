package com.sage.app.actions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sage.app.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** AlarmManager alarms are wiped on reboot; re-arm every persisted pending reminder. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(context).reminderDao()
                val scheduler = ReminderScheduler(context)
                val now = System.currentTimeMillis()
                dao.getPending().forEach { r ->
                    if (r.remindAt > now) scheduler.schedule(r.id, r.text, r.remindAt) else dao.setStatus(r.id, "missed")
                }
            } finally { pendingResult.finish() }
        }
    }
}
