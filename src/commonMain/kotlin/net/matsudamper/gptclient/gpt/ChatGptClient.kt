package net.matsudamper.gptclient.gpt

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import net.matsudamper.gptclient.entity.ChatGptModel
import net.matsudamper.gptclient.util.Log

class ChatGptClient(
    private val secretKey: String,
    private val endpoint: String = OPENAI_ENDPOINT,
) : ChatGptClientInterface {
    override suspend fun request(
        messages: List<ChatGptClientInterface.GptMessage>,
        format: ChatGptClientInterface.Format,
        model: ChatGptModel,
    ): ChatGptClientInterface.GptResult {
        val requestMessages = messages.map { message ->
            val role = when (message.role) {
                ChatGptClientInterface.GptMessage.Role.Assistant -> GptRequest.Role.Assistant
                ChatGptClientInterface.GptMessage.Role.System -> GptRequest.Role.System
                ChatGptClientInterface.GptMessage.Role.User -> GptRequest.Role.User
            }
            val contents = message.contents.map { content ->
                when (content) {
                    is ChatGptClientInterface.GptMessage.Content.Base64Image -> {
                        GptRequest.Content(
                            type = "image_url",
                            imageUrl = GptRequest.ImageUrl("data:image/png;base64,${content.base64}"),
                        )
                    }

                    is ChatGptClientInterface.GptMessage.Content.ImageUrl -> {
                        if (model.enableImage.not()) {
                            return ChatGptClientInterface.GptResult.Error(ChatGptClientInterface.GptResult.ErrorReason.ImageNotSupported())
                        }
                        GptRequest.Content(
                            type = "image_url",
                            imageUrl = GptRequest.ImageUrl(content.imageUrl),
                        )
                    }

                    is ChatGptClientInterface.GptMessage.Content.Text -> {
                        GptRequest.Content(
                            type = "text",
                            text = content.text,
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
                    ChatGptClientInterface.Format.Text -> "text"
                    ChatGptClientInterface.Format.Json -> "json_object"
                },
            ),
            topP = 1.0,
            temperature = model.requireTemperature ?: 0.3,
            maxCompletionTokens = model.defaultToken,
            frequencyPenalty = 0.0,
            presencePenalty = 0.0,
        )
        val jsonString = Json.encodeToString(
            GptRequest.serializer(),
            sampleGptRequest,
        )
        Log.d("Request", jsonString)
        val response: HttpResponse = withContext(Dispatchers.IO) {
            HttpClient(CIO) {
                install(HttpTimeout) {
                    requestTimeoutMillis = 60 * 2 * 1000L
                }
            }.use {
                it.post(endpoint) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                    header(HttpHeaders.Authorization, "Bearer $secretKey")
                    setBody(jsonString)
                }
            }
        }
        val responseJson = response.bodyAsText()
        Log.d("Response", responseJson)
        return try {
            ChatGptClientInterface.GptResult.Success(Json.decodeFromString(GptResponse.serializer(), responseJson))
        } catch (e: SerializationException) {
            e.printStackTrace()
            ChatGptClientInterface.GptResult.Error(ChatGptClientInterface.GptResult.ErrorReason.Unknown(e.message ?: "Unknown Error"))
        }
    }

    companion object {
        const val OPENAI_ENDPOINT = "https://api.openai.com/v1/chat/completions"
        const val GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"

        private val Json = Json {
            ignoreUnknownKeys = true
        }
    }
}
