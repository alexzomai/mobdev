package io.github.mobdev.network

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenStore: TokenStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenStore.token
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("X-Auth-Token", token)
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
