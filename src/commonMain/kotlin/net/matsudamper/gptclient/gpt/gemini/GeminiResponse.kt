package net.matsudamper.gptclient.gpt.gemini

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeminiResponse(
    @SerialName("candidates") val candidates: List<Candidate>,
    @SerialName("usageMetadata") val usageMetadata: UsageMetadata? = null,
    @SerialName("modelVersion") val modelVersion: String? = null,
) {
    @Serializable
    data class Candidate(
        @SerialName("content") val content: CandidateContent,
        @SerialName("finishReason") val finishReason: String? = null,
        @SerialName("index") val index: Int? = null,
    )

    @Serializable
    data class CandidateContent(
        @SerialName("parts") val parts: List<Part>,
        @SerialName("role") val role: String? = null,
    )

    @Serializable
    data class Part(
        @SerialName("text") val text: String? = null,
    )

    @Serializable
    data class UsageMetadata(
        @SerialName("promptTokenCount") val promptTokenCount: Int? = null,
        @SerialName("candidatesTokenCount") val candidatesTokenCount: Int? = null,
        @SerialName("totalTokenCount") val totalTokenCount: Int? = null,
    )
}
