package ru.emink.chatviewer

import android.net.Uri
import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

private const val BASE_URL = "https://faerytea.name/"

data class ChatMessage(
    val id: Long,
    val from: String?,
    val data: MessageData,
)

data class MessageData(
    @SerializedName("Text") val text: TextMessage?,
    @SerializedName("Image") val image: ImageMessage?,
)

data class TextMessage(val text: String)

data class ImageMessage(val link: String)

interface ChatService {
    @GET("channels")
    suspend fun channels(): List<String>

    @GET("channel/{channel}")
    suspend fun messages(
        @Path("channel") channel: String,
        @Query("lastKnownId") lastKnownId: Long,
        @Query("limit") limit: Int,
        @Query("reverse") reverse: Boolean,
    ): List<ChatMessage>
}

object ChatApi {
    val service: ChatService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ChatService::class.java)
}

fun thumbnailUrl(path: String): String = BASE_URL + "thumb/" + Uri.encode(path, "/")

fun imageUrl(path: String): String = BASE_URL + "img/" + Uri.encode(path, "/")
