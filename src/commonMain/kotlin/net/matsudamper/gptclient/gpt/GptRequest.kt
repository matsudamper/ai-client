package net.matsudamper.gptclient.gpt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.matsudamper.gptclient.serialization.StringEnum

@Serializable
data class GptRequest(
    @SerialName("model") val model: String,
    @SerialName("messages") val messages: List<Message>,
    @SerialName("response_format") val responseFormat: ResponseFormat,
    @SerialName("temperature") val temperature: Double,
    @SerialName("max_completion_tokens") val maxCompletionTokens: Int,
    @SerialName("top_p") val topP: Double,
    @SerialName("frequency_penalty") val frequencyPenalty: Double,
    @SerialName("presence_penalty") val presencePenalty: Double
) {
    @Serializable
    data class Message(
        @SerialName("role") val role: Role?,
        @SerialName("content") val content: List<Content>
    )

    @Serializable(Role.Companion.Serializer::class)
    enum class Role(override val label: String) : StringEnum {
        System("system"),
        User("user"),
        Assistant("assistant");

        companion object {
            internal object Serializer : StringEnum.Companion.Serializer<Role>(Role::class)
        }
    }

    @Serializable
    data class Content(
        @SerialName("type") val type: String,
        @SerialName("text") val text: String? = null,
        @SerialName("image_url") val imageUrl: ImageUrl? = null
    )

    @Serializable
    data class ImageUrl(
        @SerialName("url") val url: String
    )

    @Serializable
    data class ResponseFormat(
        @SerialName("type") val type: String
    )
}
