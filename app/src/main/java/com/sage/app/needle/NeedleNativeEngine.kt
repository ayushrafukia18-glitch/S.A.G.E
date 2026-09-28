package com.sage.app.needle

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class NeedleNativeEngine(private val context: Context) : NeedleEngine {
    private val moshi = Moshi.Builder().build()
    private val resultAdapter = moshi.adapter(NeedleResult::class.java)
    private var initialized = false
    private var closed = false

    override suspend fun initialize(): Result<Unit> = runCatching {
        check(!closed) { "Needle engine is closed" }
        if (initialized) return@runCatching
        val modelDir = File(context.filesDir, "needle").apply { mkdirs() }
        val model = File(modelDir, "needle3.cact")
        if (!model.exists() || model.length() < 1_000_000L) {
            context.assets.open("needle3.cact").use { input -> model.outputStream().use { input.copyTo(it) } }
        }
        val tools = NeedleToolCatalog.coreSchemas()
        val toolsType = Types.newParameterizedType(List::class.java, NeedleToolSchema::class.java)
        val toolsJson = moshi.adapter<List<NeedleToolSchema>>(toolsType).toJson(tools)
        val system = "SAGE local action router. Current date: ${ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)}. Emit only documented tool calls. Never invent contact names, message text, note text, or reminder times."
        val rc = NeedleBridge.nativeInit(model.absolutePath, system, toolsJson, null)
        if (rc < 0) error(NeedleBridge.nativeLastError() ?: "Needle init failed ($rc)")
        initialized = true
    }.onFailure { initialized = false }

    override fun isAvailable(): Boolean = initialized && !closed

    override suspend fun route(request: String, tools: List<NeedleToolSchema>, system: String?): NeedleResult {
        if (!initialized) return NeedleResult("respond", false, error = "Needle is not initialized", errorCode = NeedleIntegrationErrorCode.NEEDLE_ENGINE_LOAD_FAILED.name)
        return runCatching {
            val raw = NeedleBridge.nativeRoute(request, 256)
            resultAdapter.fromJson(raw) ?: error("Invalid Needle response")
        }.getOrElse {
            NeedleResult("respond", false, error = it.message ?: "Needle route failed", errorCode = NeedleIntegrationErrorCode.NEEDLE_ENGINE_LOAD_FAILED.name)
        }
    }

    override fun close() {
        if (!closed) {
            runCatching { NeedleBridge.nativeClose() }
            closed = true
            initialized = false
        }
    }
}

object NeedleToolCatalog {
    fun coreSchemas(): List<NeedleToolSchema> = listOf(
        NeedleToolSchema("call_contact", "Call a person from the user's contacts by name.", NeedleJsonSchemaObject(
            properties = mapOf("contact_name" to NeedleJsonSchemaProperty("string", "Contact name exactly as spoken")),
            required = listOf("contact_name")
        )),
        NeedleToolSchema("send_sms", "Send an SMS to a person from the user's contacts.", NeedleJsonSchemaObject(
            properties = mapOf(
                "contact_name" to NeedleJsonSchemaProperty("string", "Contact name exactly as spoken"),
                "message_body" to NeedleJsonSchemaProperty("string", "Message body copied from the request")
            ), required = listOf("contact_name", "message_body")
        )),
        NeedleToolSchema("set_reminder", "Set a reminder at a specific date and time.", NeedleJsonSchemaObject(
            properties = mapOf(
                "text" to NeedleJsonSchemaProperty("string", "Reminder text"),
                "datetime_iso" to NeedleJsonSchemaProperty("string", "ISO-8601 date-time")
            ), required = listOf("text", "datetime_iso")
        )),
        NeedleToolSchema("web_search_answer", "Answer a question that needs current, live web information (news, scores, prices, recent events).", NeedleJsonSchemaObject(
            properties = mapOf("query" to NeedleJsonSchemaProperty("string", "The search question exactly as asked")),
            required = listOf("query")
        )),
        NeedleToolSchema("add_note", "Save a short note locally on the device.", NeedleJsonSchemaObject(
            properties = mapOf("text" to NeedleJsonSchemaProperty("string", "Note text")),
            required = listOf("text")
        ))
    )
}
