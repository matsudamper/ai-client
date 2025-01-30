package net.matsudamper.gptclient.gpt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.matsudamper.gptclient.serialization.StringEnum

@Serializable
data class GptRequest(
    @SerialName("model") val model: String,
    @SerialName("messages") val messages: List<Message>,
    @SerialName("response_format") val responseFormat: ResponseFormat,
    /**
     * ランダム性
     * 0(決定的) ~ 1(創造的)
     */
    @SerialName("temperature") val temperature: Double,
    /**
     * どこまでを回答とするかの足切り
     * 0 ~ 1(100%以上を選択)
     */
    @SerialName("top_p") val topP: Double,
    /**
     * 生成する応答の最大トークン数
     * ~150
     */
    @SerialName("max_completion_tokens") val maxCompletionTokens: Int,
    /**
     * 同じトークンの繰り返し使用を抑制するためのペナルティ
     * -2.0(繰り返す) ~ 2.0(繰り返さない)
     */
    @SerialName("frequency_penalty") val frequencyPenalty: Double,
    /**
     * 新しいトピックや単語の導入を促進するためのペナルティ
     * -2.0(導入を曽於区心) ~ 2.0(導入を抑制)
     */
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
