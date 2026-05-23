package io.github.mobdev.network

import io.github.mobdev.data.LoginRequest
import io.github.mobdev.data.Message
import io.github.mobdev.data.SendMessageRequest
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApi {

    @POST("login")
    suspend fun login(@Body request: LoginRequest): ResponseBody

    @POST("logout")
    suspend fun logout()

    @GET("channels")
    suspend fun getChannels(): List<String>

    @GET("channel/{name}")
    suspend fun getMessages(
        @Path("name") channel: String,
        @Query("limit") limit: Int = 20,
        @Query("lastKnownId") lastKnownId: String = "0",
        @Query("reverse") reverse: Boolean = false,
    ): List<Message>

    @POST("messages")
    suspend fun sendMessage(@Body request: SendMessageRequest): ResponseBody
}
