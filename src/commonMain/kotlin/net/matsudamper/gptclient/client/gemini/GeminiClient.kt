package net.matsudamper.gptclient.client.gemini

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
import net.matsudamper.gptclient.client.AiClient
import net.matsudamper.gptclient.util.Log

class GeminiClient(
    private val apiKey: String,
) : AiClient {
    override suspend fun request(
        messages: List<AiClient.GptMessage>,
        format: AiClient.Format,
        model: ChatGptModel,
    ): AiClient.GptResult {
        val systemInstruction = messages
            .filter { it.role == AiClient.GptMessage.Role.System }
            .flatMap { message ->
                message.contents.mapNotNull { content ->
                    when (content) {
                        is AiClient.GptMessage.Content.Text -> GeminiRequest.Part(text = content.text)
                        else -> null
                    }
                }
            }
            .takeIf { it.isNotEmpty() }
            ?.let { GeminiRequest.Content(parts = it) }

        val contents = messages
            .filter { it.role != AiClient.GptMessage.Role.System }
            .map { message ->
                val role = when (message.role) {
                    AiClient.GptMessage.Role.User -> "user"
                    AiClient.GptMessage.Role.Assistant -> "model"
                    AiClient.GptMessage.Role.System -> "user"
                }
                val parts = message.contents.map { content ->
                    when (content) {
                        is AiClient.GptMessage.Content.Text -> {
                            GeminiRequest.Part(text = content.text)
                        }

                        is AiClient.GptMessage.Content.Base64Image -> {
                            GeminiRequest.Part(
                                inlineData = GeminiRequest.InlineData(
                                    mimeType = "image/png",
                                    data = content.base64,
                                ),
                            )
                        }

                        is AiClient.GptMessage.Content.ImageUrl -> {
                            if (model.enableImage.not()) {
                                return AiClient.GptResult.Error(
                                    AiClient.GptResult.ErrorReason.ImageNotSupported(),
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

        val thinkingConfig = model.thinkingLevel?.let { level ->
            GeminiRequest.ThinkingConfig(thinkingLevel = level)
        }

        val geminiRequest = GeminiRequest(
            contents = contents,
            systemInstruction = systemInstruction,
            generationConfig = GeminiRequest.GenerationConfig(
                temperature = model.requireTemperature ?: 0.3,
                topP = 1.0,
                maxOutputTokens = model.defaultToken,
                responseMimeType = when (format) {
                    AiClient.Format.Text -> "text/plain"
                    AiClient.Format.Json -> "application/json"
                },
                thinkingConfig = thinkingConfig,
            ),
        )

        val jsonString = RequestJson.encodeToString(GeminiRequest.serializer(), geminiRequest)
        Log.d("GeminiRequest", jsonString)

        val endpoint = "$GEMINI_ENDPOINT/${model.apiModelName}:generateContent?key=$apiKey"

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
            if (geminiResponse.error != null) {
                AiClient.GptResult.Error(
                    AiClient.GptResult.ErrorReason.Unknown(
                        geminiResponse.error.message ?: "Gemini API Error: ${geminiResponse.error.status}",
                    ),
                )
            } else if (geminiResponse.candidates.isEmpty()) {
                AiClient.GptResult.Error(
                    AiClient.GptResult.ErrorReason.Unknown("No response candidates returned"),
                )
            } else {
                AiClient.GptResult.Success(geminiResponse.toAiResponse())
            }
        } catch (e: SerializationException) {
            e.printStackTrace()
            AiClient.GptResult.Error(
                AiClient.GptResult.ErrorReason.Unknown(e.message ?: "Unknown Error"),
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

        private fun GeminiResponse.toAiResponse(): AiClient.AiResponse {
            return AiClient.AiResponse(
                choices = candidates.mapNotNull { candidate ->
                    val content = candidate.content ?: return@mapNotNull null
                    val textContent = content.parts
                        .filter { it.thought != true }
                        .mapNotNull { it.text }
                        .joinToString("")
                    val role = when (content.role) {
                        "model" -> AiClient.AiResponse.Choice.Role.Assistant
                        "user" -> AiClient.AiResponse.Choice.Role.User
                        else -> AiClient.AiResponse.Choice.Role.Assistant
                    }
                    AiClient.AiResponse.Choice(
                        message = AiClient.AiResponse.Choice.Message(
                            role = role,
                            content = textContent,
                        ),
                    )
                },
            )
        }
    }
}
