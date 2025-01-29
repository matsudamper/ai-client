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
import kotlinx.serialization.json.Json

class ChatGptClient(
    private val secretKey: String,
) {
    suspend fun request() {
        val sampleGptRequest = GptRequest(
            model = "gpt-4o-mini",
            messages = listOf(
                GptRequest.Message(
                    role = GptRequest.Role.User,
                    content = listOf(
                        GptRequest.Content(type = "text", text = "Hello, how are you?")
                    )
                )
            ),
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
        response.bodyAsText()
    }
}
