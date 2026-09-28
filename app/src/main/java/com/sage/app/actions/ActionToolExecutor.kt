package com.sage.app.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.sage.app.data.local.AppDatabase
import com.sage.app.data.local.NoteEntity
import com.sage.app.data.local.ReminderEntity
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.DateTimeParseException

class ActionToolExecutor(
    private val context: Context,
    private val database: AppDatabase,
    private val contactResolver: ContactResolver = ContactResolver(context),
    private val reminderScheduler: ReminderScheduler = ReminderScheduler(context),
    private val smsSender: SmsSender = SmsSender(context),
) {
    /** Only whitelisted tool names are ever executed; anything else is refused. */
    suspend fun prepare(toolName: String, args: Map<String, Any?>): ActionResult = when (toolName) {
        "call_contact" -> {
            val name = args["contact_name"]?.toString().orEmpty()
            val contact = contactResolver.resolve(name).getOrElse { return ActionResult.Reply(it.message ?: "Contact not found.") }
            ActionResult.Confirmation(PendingAction.Call(contact.displayName, contact.phoneNumber))
        }
        "send_sms" -> {
            val name = args["contact_name"]?.toString().orEmpty()
            val body = args["message_body"]?.toString().orEmpty().trim()
            if (body.isBlank()) return ActionResult.Reply("What message should I send?")
            val contact = contactResolver.resolve(name).getOrElse { return ActionResult.Reply(it.message ?: "Contact not found.") }
            ActionResult.Confirmation(PendingAction.Sms(contact.displayName, contact.phoneNumber, body))
        }
        "set_reminder" -> setReminder(args)
        "add_note" -> {
            val text = args["text"]?.toString().orEmpty().trim()
            if (text.isBlank()) ActionResult.Reply("What should I save as a note?")
            else runCatching { database.noteDao().insert(NoteEntity(text = text)) }.fold(
                onSuccess = { ActionResult.Reply("Note saved.") },
                onFailure = { ActionResult.Reply("I couldn't save that note: ${it.message ?: "storage error"}") }
            )
        }
        else -> ActionResult.Reply("I can't perform that action.")
    }

    /** Success text is produced only after the platform reports success. */
    suspend fun confirm(action: PendingAction): Result<String> = when (action) {
        is PendingAction.Call -> runCatching {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(action.phoneNumber)}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "Opened the dialer for ${action.contactName}. Press call to place it."
        }
        is PendingAction.Sms -> {
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
                Result.failure(IllegalStateException("SMS permission is required before I can send this message."))
            else smsSender.send(action.phoneNumber, action.body).map { "Message sent to ${action.contactName}." }
        }
    }

    private suspend fun setReminder(args: Map<String, Any?>): ActionResult {
        val text = args["text"]?.toString().orEmpty().trim()
        val iso = args["datetime_iso"]?.toString().orEmpty().trim()
        if (text.isBlank() || iso.isBlank()) return ActionResult.Reply("I need the reminder text and date/time.")
        val millis = parseMillis(iso) ?: return ActionResult.Reply("I couldn't understand that reminder time.")
        if (millis <= System.currentTimeMillis()) return ActionResult.Reply("That reminder time has already passed.")
        return runCatching {
            val id = database.reminderDao().insert(ReminderEntity(text = text, remindAt = millis))
            val exact = reminderScheduler.schedule(id, text, millis)
            val whenText = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
            if (exact) "Reminder set for $whenText." else "Reminder set for about $whenText (exact alarms are not allowed for SAGE, so it may be slightly late)."
        }.fold({ ActionResult.Reply(it) }, { ActionResult.Reply("I couldn't set that reminder: ${it.message ?: "scheduling error"}") })
    }

    companion object {
        /** Accepts Z / offset ISO strings and offset-less local date-times (interpreted in the device zone). */
        fun parseMillis(iso: String): Long? {
            try { return OffsetDateTime.parse(iso).toInstant().toEpochMilli() } catch (_: DateTimeParseException) {}
            try { return LocalDateTime.parse(iso).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() } catch (_: DateTimeParseException) {}
            return null
        }
    }
}
