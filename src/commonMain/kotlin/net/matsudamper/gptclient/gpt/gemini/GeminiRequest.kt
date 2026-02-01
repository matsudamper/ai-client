package net.matsudamper.gptclient.gpt.gemini

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeminiRequest(
    @SerialName("contents") val contents: List<Content>,
    @SerialName("systemInstruction") val systemInstruction: Content? = null,
    @SerialName("generationConfig") val generationConfig: GenerationConfig,
) {
    @Serializable
    data class Content(
        @SerialName("role") val role: String? = null,
        @SerialName("parts") val parts: List<Part>,
    )

    @Serializable
    data class Part(
        @SerialName("text") val text: String? = null,
        @SerialName("inlineData") val inlineData: InlineData? = null,
    )

    @Serializable
    data class InlineData(
        @SerialName("mimeType") val mimeType: String,
        @SerialName("data") val data: String,
    )

    @Serializable
    data class GenerationConfig(
        @SerialName("temperature") val temperature: Double,
        @SerialName("topP") val topP: Double,
        @SerialName("maxOutputTokens") val maxOutputTokens: Int,
        @SerialName("responseMimeType") val responseMimeType: String,
    )
}
