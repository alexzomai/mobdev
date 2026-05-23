package io.github.mobdev.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MessageCache(context: Context) {

    private val prefs = context.getSharedPreferences("message_cache", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun save(channel: String, messages: List<Message>) {
        val toSave = if (messages.size > MAX_CACHED) messages.takeLast(MAX_CACHED) else messages
        prefs.edit().putString(keyFor(channel), json.encodeToString(toSave)).apply()
    }

    fun load(channel: String): List<Message> {
        val raw = prefs.getString(keyFor(channel), null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<Message>>(raw) }.getOrDefault(emptyList())
    }

    private fun keyFor(channel: String) = "ch_$channel"

    companion object {
        private const val MAX_CACHED = 100
    }
}
