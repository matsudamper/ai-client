package net.matsudamper.gptclient.gpt

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class ChatGptClient(
    private val secretKey: String,
) {
    suspend fun request(messages: List<GptMessage>): GptResponse {
        val requestMessages = messages.map { message ->
            val role = when (message.role) {
                GptMessage.Role.Assistant -> GptRequest.Role.Assistant
                GptMessage.Role.System -> GptRequest.Role.System
                GptMessage.Role.User -> GptRequest.Role.User
            }
            val contents = message.contents.map { content ->
                when (content) {
                    is GptMessage.Content.Base64Image -> {
                        GptRequest.Content(
                            type = "image_url",
                            imageUrl = GptRequest.ImageUrl("data:image/bmp;base64,${content.base64}"),
                        )
                    }

                    is GptMessage.Content.ImageUrl -> {
                        GptRequest.Content(
                            type = "image_url",
                            imageUrl = GptRequest.ImageUrl(content.imageUrl)
                        )
                    }

                    is GptMessage.Content.Text -> {
                        GptRequest.Content(
                            type = "text",
                            text = content.text
                        )
                    }
                }
            }

            GptRequest.Message(
                role = role,
                content = contents,
            )
        }
        val sampleGptRequest = GptRequest(
            model = "gpt-4o-mini",
            messages = requestMessages,
            responseFormat = GptRequest.ResponseFormat(type = "text"),
            temperature = 0.7,
            maxCompletionTokens = 100,
            topP = 1.0,
            frequencyPenalty = 0.0,
            presencePenalty = 0.0
        )
        val jsonString = Json.encodeToString(
            GptRequest.serializer(),
            sampleGptRequest
        )

        val response: HttpResponse = withContext(Dispatchers.IO) {
            HttpClient {}.use {
                it.post("https://api.openai.com/v1/chat/completions") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                    header(HttpHeaders.Authorization, "Bearer $secretKey")
                    setBody(jsonString)
                }
            }
        }
        val responseJson = response.bodyAsText()
        return try {
            Json.decodeFromString(GptResponse.serializer(), responseJson)
        } catch (e: SerializationException) {
            System.err.println(responseJson)
            throw e
        }
    }

    data class GptMessage(
        val role: Role,
        val contents: List<Content>,
    ) {
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
}
