package io.github.mobdev.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mobdev.network.ApiClient
import io.github.mobdev.network.TokenStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import retrofit2.HttpException

class ChatListViewModel(
    private val tokenStore: TokenStore,
    context: Context,
) : ViewModel() {

    private val api = ApiClient.create(tokenStore)
    private val prefs = context.getSharedPreferences("channel_cache", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _channels = MutableStateFlow<List<String>>(emptyList())
    val channels: StateFlow<List<String>> = _channels

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _unauthorized = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val unauthorized: SharedFlow<Unit> = _unauthorized

    private var loadedOnce = false

    fun ensureLoaded() {
        if (loadedOnce) return
        loadedOnce = true
        val cached = loadCached()
        if (cached.isNotEmpty()) _channels.value = cached
        loadChannels()
    }

    fun reset() {
        loadedOnce = false
        _channels.value = emptyList()
        _isLoading.value = false
    }

    private fun loadChannels() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val channels = api.getChannels()
                _channels.value = channels
                saveCache(channels)
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    loadedOnce = false
                    _unauthorized.tryEmit(Unit)
                }
            } catch (_: Exception) {
                loadedOnce = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadCached(): List<String> {
        val raw = prefs.getString(KEY_CHANNELS, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())
    }

    private fun saveCache(channels: List<String>) {
        prefs.edit().putString(KEY_CHANNELS, json.encodeToString(channels)).apply()
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                api.logout()
            } catch (_: Exception) {
            }
            tokenStore.clear()
            onDone()
        }
    }

    companion object {
        private const val KEY_CHANNELS = "channels"
    }
}
