package io.github.mobdev.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mobdev.data.LoginRequest
import io.github.mobdev.network.ApiClient
import io.github.mobdev.network.TokenStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

class LoginViewModel(private val tokenStore: TokenStore) : ViewModel() {

    private val api = ApiClient.create(tokenStore)

    sealed class State {
        data object Idle : State()
        data object Loading : State()
        data object Success : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state

    fun login(name: String, password: String) {
        viewModelScope.launch {
            _state.value = State.Loading
            try {
                val body = api.login(LoginRequest(name, password)).string()
                val token = parseToken(body)
                tokenStore.token = token
                tokenStore.login = name
                tokenStore.password = password
                _state.value = State.Success
            } catch (e: HttpException) {
                _state.value = if (e.code() == 401) {
                    State.Error("wrong_credentials")
                } else {
                    State.Error("network")
                }
            } catch (e: Exception) {
                _state.value = State.Error("network")
            }
        }
    }

    private fun parseToken(body: String): String {
        return try {
            Json.parseToJsonElement(body).jsonObject["token"]?.jsonPrimitive?.content
                ?: body.trim()
        } catch (e: Exception) {
            body.trim()
        }
    }
}
