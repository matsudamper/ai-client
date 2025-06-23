package net.matsudamper.gptclient.gpt

import net.matsudamper.gptclient.entity.ChatGptModel

interface ChatGptClientInterface {
    suspend fun request(
        messages: List<GptMessage>,
        format: Format,
        model: ChatGptModel,
    ): GptResult

    sealed interface GptResult {
        data class Success(val response: GptResponse) : GptResult
        data class Error(val reason: ErrorReason) : GptResult

        sealed interface ErrorReason {
            val message: String

            data class ImageNotSupported(override val message: String = "画像をサポートしていないモデルです") : ErrorReason

            data class Unknown(override val message: String) : ErrorReason
        }
    }

    data class GptMessage(val role: Role, val contents: List<Content>) {
        enum class Role {
            User,
            Assistant,
            System,
        }

        sealed interface Content {
            data class Text(val text: String) : Content
            data class ImageUrl(val imageUrl: String) : Content
            data class Base64Image(val base64: String) : Content
        }
    }

    enum class Format {
        Text,
        Json,
    }
} 
