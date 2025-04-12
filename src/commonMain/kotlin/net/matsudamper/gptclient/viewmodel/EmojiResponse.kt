package net.matsudamper.gptclient.viewmodel

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EmojiGptResponse(
    @SerialName("error_message") val errorMessage: String? = null,
    @SerialName("results") val results: List<String>,
)
