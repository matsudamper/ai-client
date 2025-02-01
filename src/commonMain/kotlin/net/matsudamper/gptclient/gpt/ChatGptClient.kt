package net.matsudamper.gptclient.gpt

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.timeout
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
import net.matsudamper.gptclient.entity.ChatGptModel

class ChatGptClient(
    private val secretKey: String,
) {
    suspend fun request(
        messages: List<GptMessage>,
        format: Format,
        model: ChatGptModel,
    ): GptResult {
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
                            imageUrl = GptRequest.ImageUrl("data:image/png;base64,${content.base64}"),
                        )
                    }

                    is GptMessage.Content.ImageUrl -> {
                        if (model.enableImage.not()) {
                            return GptResult.Error(GptResult.ErrorReason.ImageNotSupported())
                        }
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
            model = model.modelName,
            messages = requestMessages,
            responseFormat = GptRequest.ResponseFormat(
                type = when (format) {
                    Format.Text -> "text"
                    Format.Json -> "json_object"
                }
            ),
            topP = 1.0,
            temperature = 0.3,
            maxCompletionTokens = 500,
            frequencyPenalty = 0.0,
            presencePenalty = 0.0
        )
        val jsonString = Json.encodeToString(
            GptRequest.serializer(),
            sampleGptRequest
        )
        println("Request->$jsonString")
        val response: HttpResponse = withContext(Dispatchers.IO) {
            HttpClient(CIO) {
                install(HttpTimeout) {
                    requestTimeoutMillis = 60 * 2 * 1000L
                }
            }.use {
                it.post("https://api.openai.com/v1/chat/completions") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                    header(HttpHeaders.Authorization, "Bearer $secretKey")
                    setBody(jsonString)
                }
            }
        }
        val responseJson = response.bodyAsText()
        println("Response->${responseJson}")
        return try {
            GptResult.Success(Json.decodeFromString(GptResponse.serializer(), responseJson))
        } catch (e: SerializationException) {
            System.err.println(responseJson)
            GptResult.Error(GptResult.ErrorReason.Unknown(e.message ?: "Unknown Error"))
        }
    }

    sealed interface GptResult {
        data class Success(val response: GptResponse) : GptResult
        data class Error(val reason: ErrorReason) : GptResult

        sealed interface ErrorReason {
            val message: String
            data class ImageNotSupported(
                override val message: String = "画像をサポートしていないモデルです"
            ) : ErrorReason
            data class Unknown(override val message: String) : ErrorReason
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

    enum class Format {
        Text,
        Json,
    }
}
