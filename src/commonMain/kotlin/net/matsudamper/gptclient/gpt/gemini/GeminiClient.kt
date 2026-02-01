package net.matsudamper.gptclient.gpt.gemini

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import net.matsudamper.gptclient.entity.ChatGptModel
import net.matsudamper.gptclient.gpt.ChatGptClientInterface
import net.matsudamper.gptclient.util.Log

class GeminiClient(
    private val apiKey: String,
) : ChatGptClientInterface {
    override suspend fun request(
        messages: List<ChatGptClientInterface.GptMessage>,
        format: ChatGptClientInterface.Format,
        model: ChatGptModel,
    ): ChatGptClientInterface.GptResult {
        val systemInstruction = messages
            .filter { it.role == ChatGptClientInterface.GptMessage.Role.System }
            .flatMap { message ->
                message.contents.mapNotNull { content ->
                    when (content) {
                        is ChatGptClientInterface.GptMessage.Content.Text -> GeminiRequest.Part(text = content.text)
                        else -> null
                    }
                }
            }
            .takeIf { it.isNotEmpty() }
            ?.let { GeminiRequest.Content(parts = it) }

        val contents = messages
            .filter { it.role != ChatGptClientInterface.GptMessage.Role.System }
            .map { message ->
                val role = when (message.role) {
                    ChatGptClientInterface.GptMessage.Role.User -> "user"
                    ChatGptClientInterface.GptMessage.Role.Assistant -> "model"
                    ChatGptClientInterface.GptMessage.Role.System -> "user"
                }
                val parts = message.contents.map { content ->
                    when (content) {
                        is ChatGptClientInterface.GptMessage.Content.Text -> {
                            GeminiRequest.Part(text = content.text)
                        }

                        is ChatGptClientInterface.GptMessage.Content.Base64Image -> {
                            GeminiRequest.Part(
                                inlineData = GeminiRequest.InlineData(
                                    mimeType = "image/png",
                                    data = content.base64,
                                ),
                            )
                        }

                        is ChatGptClientInterface.GptMessage.Content.ImageUrl -> {
                            if (model.enableImage.not()) {
                                return ChatGptClientInterface.GptResult.Error(
                                    ChatGptClientInterface.GptResult.ErrorReason.ImageNotSupported(),
                                )
                            }
                            GeminiRequest.Part(
                                inlineData = GeminiRequest.InlineData(
                                    mimeType = "image/png",
                                    data = content.imageUrl,
                                ),
                            )
                        }
                    }
                }
                GeminiRequest.Content(role = role, parts = parts)
            }

        val geminiRequest = GeminiRequest(
            contents = contents,
            systemInstruction = systemInstruction,
            generationConfig = GeminiRequest.GenerationConfig(
                temperature = model.requireTemperature ?: 0.3,
                topP = 1.0,
                maxOutputTokens = model.defaultToken,
                responseMimeType = when (format) {
                    ChatGptClientInterface.Format.Text -> "text/plain"
                    ChatGptClientInterface.Format.Json -> "application/json"
                },
            ),
        )

        val jsonString = RequestJson.encodeToString(GeminiRequest.serializer(), geminiRequest)
        Log.d("GeminiRequest", jsonString)

        val endpoint = "$GEMINI_ENDPOINT/${model.modelName}:generateContent?key=$apiKey"

        val response: HttpResponse = withContext(Dispatchers.IO) {
            HttpClient(CIO) {
                install(HttpTimeout) {
                    requestTimeoutMillis = 60 * 2 * 1000L
                }
            }.use {
                it.post(endpoint) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                    setBody(jsonString)
                }
            }
        }
        val responseJson = response.bodyAsText()
        Log.d("GeminiResponse", responseJson)

        return try {
            val geminiResponse = ResponseJson.decodeFromString(GeminiResponse.serializer(), responseJson)
            ChatGptClientInterface.GptResult.Success(geminiResponse.toAiResponse())
        } catch (e: SerializationException) {
            e.printStackTrace()
            ChatGptClientInterface.GptResult.Error(
                ChatGptClientInterface.GptResult.ErrorReason.Unknown(e.message ?: "Unknown Error"),
            )
        }
    }

    companion object {
        private const val GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models"

        private val RequestJson = Json {
            encodeDefaults = true
            explicitNulls = false
        }

        private val ResponseJson = Json {
            ignoreUnknownKeys = true
        }

        private fun GeminiResponse.toAiResponse(): ChatGptClientInterface.AiResponse {
            return ChatGptClientInterface.AiResponse(
                choices = candidates.map { candidate ->
                    val textContent = candidate.content.parts
                        .mapNotNull { it.text }
                        .joinToString("")
                    val role = when (candidate.content.role) {
                        "model" -> ChatGptClientInterface.AiResponse.Choice.Role.Assistant
                        "user" -> ChatGptClientInterface.AiResponse.Choice.Role.User
                        else -> ChatGptClientInterface.AiResponse.Choice.Role.Assistant
                    }
                    ChatGptClientInterface.AiResponse.Choice(
                        message = ChatGptClientInterface.AiResponse.Choice.Message(
                            role = role,
                            content = textContent,
                        ),
                    )
                },
            )
        }
    }
}
