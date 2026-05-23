package io.github.mobdev.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class AppViewModel(private val state: SavedStateHandle) : ViewModel() {

    val selectedChannel: StateFlow<String?> = state.getStateFlow(KEY_CHANNEL, null)

    private val _unauthorized = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val unauthorized: SharedFlow<Unit> = _unauthorized

    fun selectChannel(channel: String?) {
        state[KEY_CHANNEL] = channel
    }

    fun notifyUnauthorized() {
        _unauthorized.tryEmit(Unit)
    }

    companion object {
        private const val KEY_CHANNEL = "selected_channel"
    }
}
