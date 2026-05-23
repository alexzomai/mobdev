package io.github.mobdev.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Serializable
data class LoginRequest(
    val name: String,
    val pwd: String,
)


@Serializable
data class Message(
    @Serializable(with = FlexibleStringSerializer::class)
    val id: String = "",
    val from: String = "",
    val to: String = "1@channel",
    @Serializable(with = MessageDataSerializer::class)
    val data: MessageData,
    @Serializable(with = FlexibleStringSerializer::class)
    val time: String = "",
)

object FlexibleStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }

    override fun deserialize(decoder: Decoder): String =
        if (decoder is JsonDecoder) {
            decoder.decodeJsonElement().jsonPrimitive.content
        } else {
            decoder.decodeString()
        }
}

sealed class MessageData {
    data class Text(val text: String) : MessageData()
    data class Image(val link: String) : MessageData()
}

object MessageDataSerializer : KSerializer<MessageData> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MessageData")

    override fun serialize(encoder: Encoder, value: MessageData) {
        val jsonEncoder = encoder as JsonEncoder
        val json = when (value) {
            is MessageData.Text -> buildJsonObject {
                put("Text", buildJsonObject { put("text", value.text) })
            }
            is MessageData.Image -> buildJsonObject {
                put("Image", buildJsonObject { put("link", value.link) })
            }
        }
        jsonEncoder.encodeJsonElement(json)
    }

    override fun deserialize(decoder: Decoder): MessageData {
        val jsonDecoder = decoder as JsonDecoder
        val obj = jsonDecoder.decodeJsonElement().jsonObject
        return when {
            obj.containsKey("Text") -> {
                val text = obj["Text"]?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: ""
                MessageData.Text(text)
            }
            obj.containsKey("Image") -> {
                val link = obj["Image"]?.jsonObject?.get("link")?.jsonPrimitive?.contentOrNull ?: ""
                MessageData.Image(link)
            }
            else -> MessageData.Text("[unsupported: ${obj.keys.firstOrNull() ?: "?"}]")
        }
    }
}

@Serializable
data class SendMessageRequest(
    val from: String,
    val to: String,
    val data: @Serializable(with = MessageDataSerializer::class) MessageData,
)
