package io.github.mobdev.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mobdev.data.LOCAL_ID_PREFIX
import io.github.mobdev.data.Message
import io.github.mobdev.data.MessageCache
import io.github.mobdev.data.MessageData
import io.github.mobdev.data.PendingMessage
import io.github.mobdev.data.PendingMessageStore
import io.github.mobdev.data.SendMessageRequest
import io.github.mobdev.network.ApiClient
import io.github.mobdev.network.NetworkMonitor
import io.github.mobdev.network.TokenStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

class MessagesViewModel(
    private val tokenStore: TokenStore,
    context: Context,
) : ViewModel() {

    private val api = ApiClient.create(tokenStore)
    private val networkMonitor = NetworkMonitor(context)
    private val messageCache = MessageCache(context)
    private val pendingStore = PendingMessageStore(context)

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _canLoadMore = MutableStateFlow(true)
    val canLoadMore: StateFlow<Boolean> = _canLoadMore

    private val _unauthorized = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val unauthorized: SharedFlow<Unit> = _unauthorized

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val errors: SharedFlow<String> = _errors

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    private var currentChannel: String? = null
    private var oldestKnownId: String = NEWEST_ID
    private var newestKnownId: String = "0"

    init {
        viewModelScope.launch {
            var wasOnline = networkMonitor.isOnline.value
            networkMonitor.isOnline.collect { online ->
                if (online && !wasOnline) {
                    val ch = currentChannel
                    if (ch != null) {
                        if (newestKnownId == "0") loadInitial() else loadNew()
                        flushPending()
                    }
                }
                wasOnline = online
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        networkMonitor.unregister()
    }

    fun setChannel(channel: String) {
        if (currentChannel == channel) return
        currentChannel = channel
        _messages.value = emptyList()
        oldestKnownId = NEWEST_ID
        newestKnownId = "0"
        _canLoadMore.value = true
        _isLoading.value = false

        val cached = messageCache.load(channel)
        if (cached.isNotEmpty()) {
            newestKnownId = cached.lastOrNull()?.id ?: "0"
            oldestKnownId = cached.firstOrNull()?.id ?: NEWEST_ID
            val pending = pendingStore.loadAll()
                .filter { it.channel == channel }
                .map { it.toMessage() }
            _messages.value = cached + pending
        }

        if (networkMonitor.isOnline.value) {
            loadInitial()
        }
    }

    fun reset() {
        currentChannel = null
        _messages.value = emptyList()
        oldestKnownId = NEWEST_ID
        newestKnownId = "0"
        _canLoadMore.value = true
        _isLoading.value = false
    }

    private fun loadInitial() {
        val channel = currentChannel ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = api.getMessages(channel, limit = PAGE_SIZE, lastKnownId = NEWEST_ID, reverse = true)
                val sorted = result.reversed()
                val pending = _messages.value.filter { it.id.startsWith(LOCAL_ID_PREFIX) }
                _messages.value = sorted + pending
                newestKnownId = sorted.lastOrNull()?.id ?: newestKnownId
                oldestKnownId = sorted.firstOrNull()?.id ?: NEWEST_ID
                _canLoadMore.value = result.size >= PAGE_SIZE
                messageCache.save(channel, sorted)
            } catch (e: HttpException) {
                Log.e(TAG, "loadInitial HTTP ${e.code()}", e)
                if (e.code() == 401) _unauthorized.tryEmit(Unit)
                else _errors.tryEmit("Ошибка загрузки: HTTP ${e.code()}")
            } catch (e: Exception) {
                Log.e(TAG, "loadInitial failed", e)
                // сетевая ошибка — баннер уже показывает статус офлайн
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadNew() {
        val channel = currentChannel ?: return
        if (newestKnownId == "0") {
            loadInitial()
            return
        }
        viewModelScope.launch {
            try {
                val result = api.getMessages(
                    channel,
                    limit = PAGE_SIZE,
                    lastKnownId = newestKnownId,
                    reverse = false,
                )
                if (result.isNotEmpty()) {
                    val existingIds = _messages.value
                        .filter { !it.id.startsWith(LOCAL_ID_PREFIX) }
                        .map { it.id }
                        .toSet()
                    val newMsgs = result.filter { it.id !in existingIds }
                    if (newMsgs.isNotEmpty()) {
                        _messages.update { current ->
                            val confirmed = current.filter { !it.id.startsWith(LOCAL_ID_PREFIX) }
                            val pending = current.filter { it.id.startsWith(LOCAL_ID_PREFIX) }
                            confirmed + newMsgs + pending
                        }
                        newestKnownId = newMsgs.lastOrNull()?.id ?: newestKnownId
                        val toCache = _messages.value.filter { !it.id.startsWith(LOCAL_ID_PREFIX) }
                        messageCache.save(channel, toCache)
                    }
                }
            } catch (e: HttpException) {
                if (e.code() == 401) _unauthorized.tryEmit(Unit)
            } catch (_: Exception) {
                // silent, retry on next reconnect
            }
        }
    }

    fun loadMore() {
        val channel = currentChannel ?: return
        if (_isLoading.value || !_canLoadMore.value) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = api.getMessages(
                    channel,
                    limit = PAGE_SIZE,
                    lastKnownId = oldestKnownId,
                    reverse = true,
                )
                if (result.isEmpty()) {
                    _canLoadMore.value = false
                } else {
                    val sorted = result.reversed()
                    val existingIds = _messages.value.map { it.id }.toSet()
                    val newOlder = sorted.filter { it.id !in existingIds }
                    if (newOlder.isNotEmpty()) {
                        _messages.update { newOlder + it }
                        oldestKnownId = newOlder.firstOrNull()?.id ?: oldestKnownId
                    }
                    if (result.size < PAGE_SIZE) _canLoadMore.value = false
                }
            } catch (e: HttpException) {
                Log.e(TAG, "loadMore HTTP ${e.code()}", e)
                if (e.code() == 401) _unauthorized.tryEmit(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "loadMore failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendMessage(text: String) {
        val channel = currentChannel ?: return
        if (text.isBlank()) return
        val login = tokenStore.login ?: return

        if (!networkMonitor.isOnline.value) {
            _errors.tryEmit("Нет подключения к сети")
            return
        }

        viewModelScope.launch {
            try {
                val request = SendMessageRequest(
                    from = login,
                    to = channel,
                    data = MessageData.Text(text),
                )
                val response = api.sendMessage(request)
                val newId = response.string().trim()
                Log.d(TAG, "sendMessage ok, newId='$newId'")

                if (newId.isNotEmpty() && _messages.value.none { it.id == newId }) {
                    val newMessage = Message(
                        id = newId,
                        from = login,
                        to = channel,
                        data = MessageData.Text(text),
                    )
                    _messages.update { it + newMessage }
                    newestKnownId = newId
                }
            } catch (e: HttpException) {
                val errorBody = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
                Log.e(TAG, "sendMessage HTTP ${e.code()} body=$errorBody", e)
                if (e.code() == 401) _unauthorized.tryEmit(Unit)
                else _errors.tryEmit("Не отправлено: HTTP ${e.code()} ${errorBody.orEmpty()}")
            } catch (e: Exception) {
                Log.e(TAG, "sendMessage failed", e)
                _errors.tryEmit("Нет подключения к сети")
            }
        }
    }

    private fun flushPending() {
        val ch = currentChannel ?: return
        val pending = pendingStore.loadAll().filter { it.channel == ch }
        if (pending.isEmpty()) return
        viewModelScope.launch {
            for (pm in pending) {
                try {
                    val request = SendMessageRequest(
                        from = pm.from,
                        to = pm.channel,
                        data = MessageData.Text(pm.text),
                    )
                    val response = api.sendMessage(request)
                    val newId = response.string().trim()
                    pendingStore.remove(pm.localId)
                    if (newId.isNotEmpty()) {
                        _messages.update { list ->
                            list.map { msg ->
                                if (msg.id == LOCAL_ID_PREFIX + pm.localId) msg.copy(id = newId)
                                else msg
                            }
                        }
                        newestKnownId = newId
                    }
                } catch (e: HttpException) {
                    if (e.code() == 401) {
                        _unauthorized.tryEmit(Unit)
                        break
                    }
                    // else: keep pending for next reconnect
                } catch (_: Exception) {
                    // keep pending for next reconnect
                }
            }
        }
    }

    companion object {
        private const val PAGE_SIZE = 20
        private const val TAG = "MessagesViewModel"
        private const val NEWEST_ID = "99999999"
    }
}
