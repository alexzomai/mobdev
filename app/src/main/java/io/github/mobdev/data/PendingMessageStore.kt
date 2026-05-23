package io.github.mobdev.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class PendingMessage(
    val localId: String,
    val channel: String,
    val from: String,
    val text: String,
) {
    fun toMessage() = Message(
        id = LOCAL_ID_PREFIX + localId,
        from = from,
        to = channel,
        data = MessageData.Text(text),
    )
}

const val LOCAL_ID_PREFIX = "local_"

class PendingMessageStore(context: Context) {

    private val prefs = context.getSharedPreferences("pending_messages", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun add(msg: PendingMessage) {
        val current = loadAll().toMutableList()
        current.add(msg)
        save(current)
    }

    fun remove(localId: String) {
        val current = loadAll().filter { it.localId != localId }
        save(current)
    }

    fun loadAll(): List<PendingMessage> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<PendingMessage>>(raw) }.getOrDefault(emptyList())
    }

    private fun save(list: List<PendingMessage>) {
        prefs.edit().putString(KEY, json.encodeToString(list)).apply()
    }

    companion object {
        private const val KEY = "pending"
    }
}
