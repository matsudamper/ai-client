package net.matsudamper.gptclient.gpt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.matsudamper.gptclient.serialization.StringEnum

@Serializable
data class GptResponse(
    @SerialName("id") val id: String,
    @SerialName("object") val objectType: String,
    @SerialName("created") val created: Long,
    @SerialName("model") val model: String,
    @SerialName("choices") val choices: List<Choice>,
    @SerialName("usage") val usage: Usage,
    @SerialName("service_tier") val serviceTier: String,
    @SerialName("system_fingerprint") val systemFingerprint: String
) {
    @Serializable
    data class Choice(
        @SerialName("index") val index: Int,
        @SerialName("message") val message: Message,
        @SerialName("logprobs") val logprobs: String? = null,
        @SerialName("finish_reason") val finishReason: String
    ) {
        @Serializable
        data class Message(
            @SerialName("role") val role: Role?,
            @SerialName("content") val content: String,
            @SerialName("refusal") val refusal: String? = null
        )

        @Serializable(Role.Companion.Serializer::class)
        enum class Role(override val label: String) : StringEnum {
            System("system"),
            User("user"),
            Assistant("assistant")
            ;

            companion object {
                internal object Serializer : StringEnum.Companion.Serializer<Role>(Role::class)
            }
        }
    }

    @Serializable
    data class Usage(
        @SerialName("prompt_tokens") val promptTokens: Int,
        @SerialName("completion_tokens") val completionTokens: Int,
        @SerialName("total_tokens") val totalTokens: Int,
        @SerialName("prompt_tokens_details") val promptTokensDetails: TokenDetails,
        @SerialName("completion_tokens_details") val completionTokensDetails: TokenDetails
    ) {
        @Serializable
        data class TokenDetails(
            @SerialName("cached_tokens") val cachedTokens: Int? = null,
            @SerialName("audio_tokens") val audioTokens: Int? = null,
            @SerialName("reasoning_tokens") val reasoningTokens: Int? = null,
            @SerialName("accepted_prediction_tokens") val acceptedPredictionTokens: Int? = null,
            @SerialName("rejected_prediction_tokens") val rejectedPredictionTokens: Int? = null,
        )
    }
}
