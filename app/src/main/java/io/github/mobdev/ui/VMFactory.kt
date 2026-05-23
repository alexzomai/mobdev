package io.github.mobdev.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.mobdev.network.TokenStore

class VMFactory(
    private val tokenStore: TokenStore,
    private val context: Context,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(ChatListViewModel::class.java) ->
            ChatListViewModel(tokenStore, context) as T
        modelClass.isAssignableFrom(MessagesViewModel::class.java) ->
            MessagesViewModel(tokenStore, context) as T
        else -> throw IllegalArgumentException("Unknown ViewModel: $modelClass")
    }
}
