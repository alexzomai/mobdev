package io.github.mobdev.network

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class TokenStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit { putString(KEY_TOKEN, value) }

    var login: String?
        get() = prefs.getString(KEY_LOGIN, null)
        set(value) = prefs.edit { putString(KEY_LOGIN, value) }

    var password: String?
        get() = prefs.getString(KEY_PASSWORD, null)
        set(value) = prefs.edit { putString(KEY_PASSWORD, value) }

    fun clear() = prefs.edit { clear() }

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_LOGIN = "login"
        private const val KEY_PASSWORD = "password"
    }
}
