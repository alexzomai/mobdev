package io.github.mobdev.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.github.mobdev.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

object ApiClient {

    const val BASE_URL = "https://faerytea.name/"
    const val BASE_IMAGE_URL = BASE_URL

    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
    }

    fun create(tokenStore: TokenStore): ChatApi {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStore))
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ChatApi::class.java)
    }
}
