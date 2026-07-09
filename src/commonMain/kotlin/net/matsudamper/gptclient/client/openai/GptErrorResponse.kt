package net.matsudamper.gptclient.client.openai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GptErrorResponse(
    @SerialName("error") val error: Error,
) {
    @Serializable
    data class Error(
        @SerialName("message") val message: String? = null,
        @SerialName("type") val type: String? = null,
        @SerialName("param") val param: String? = null,
        @SerialName("code") val code: String? = null,
    )
}
