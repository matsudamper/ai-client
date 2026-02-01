package net.matsudamper.gptclient.client.openai

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
import net.matsudamper.gptclient.client.AiClient
import net.matsudamper.gptclient.entity.ChatGptModel
import net.matsudamper.gptclient.util.Log

class ChatGptClient(
    private val secretKey: String,
) : AiClient {
    override suspend fun request(
        messages: List<AiClient.GptMessage>,
        format: AiClient.Format,
        model: ChatGptModel,
    ): AiClient.GptResult {
        val requestMessages = messages.map { message ->
            val role = when (message.role) {
                AiClient.GptMessage.Role.Assistant -> GptRequest.Role.Assistant
                AiClient.GptMessage.Role.System -> GptRequest.Role.System
                AiClient.GptMessage.Role.User -> GptRequest.Role.User
            }
            val contents = message.contents.map { content ->
                when (content) {
                    is AiClient.GptMessage.Content.Base64Image -> {
                        GptRequest.Content(
                            type = "image_url",
                            imageUrl = GptRequest.ImageUrl("data:image/png;base64,${content.base64}"),
                        )
                    }

                    is AiClient.GptMessage.Content.ImageUrl -> {
                        if (model.enableImage.not()) {
                            return AiClient.GptResult.Error(AiClient.GptResult.ErrorReason.ImageNotSupported())
                        }
                        GptRequest.Content(
                            type = "image_url",
                            imageUrl = GptRequest.ImageUrl(content.imageUrl),
                        )
                    }

                    is AiClient.GptMessage.Content.Text -> {
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
                    AiClient.Format.Text -> "text"
                    AiClient.Format.Json -> "json_object"
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
                it.post(OPENAI_ENDPOINT) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                    header(HttpHeaders.Authorization, "Bearer $secretKey")
                    setBody(jsonString)
                }
            }
        }
        val responseJson = response.bodyAsText()
        Log.d("Response", responseJson)
        return try {
            val gptResponse = Json.decodeFromString(GptResponse.serializer(), responseJson)
            AiClient.GptResult.Success(gptResponse.toAiResponse())
        } catch (e: SerializationException) {
            e.printStackTrace()
            AiClient.GptResult.Error(AiClient.GptResult.ErrorReason.Unknown(e.message ?: "Unknown Error"))
        }
    }

    companion object {
        const val OPENAI_ENDPOINT = "https://api.openai.com/v1/chat/completions"

        private val Json = Json {
            ignoreUnknownKeys = true
        }

        private fun GptResponse.toAiResponse(): AiClient.AiResponse {
            return AiClient.AiResponse(
                choices = choices.map { choice ->
                    AiClient.AiResponse.Choice(
                        message = AiClient.AiResponse.Choice.Message(
                            role = when (choice.message.role) {
                                GptResponse.Choice.Role.System -> AiClient.AiResponse.Choice.Role.System
                                GptResponse.Choice.Role.User -> AiClient.AiResponse.Choice.Role.User
                                GptResponse.Choice.Role.Assistant -> AiClient.AiResponse.Choice.Role.Assistant
                                null -> null
                            },
                            content = choice.message.content,
                        ),
                    )
                },
            )
        }
    }
}
