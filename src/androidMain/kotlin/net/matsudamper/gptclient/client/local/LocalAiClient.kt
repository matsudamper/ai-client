package net.matsudamper.gptclient.client.local

import android.graphics.BitmapFactory
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerationConfig
import com.google.mlkit.genai.prompt.ImagePart
import com.google.mlkit.genai.prompt.TextPart
import net.matsudamper.gptclient.client.AiClient
import net.matsudamper.gptclient.entity.ChatGptModel
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

actual fun createLocalAiClient(): AiClient? = LocalAiClientImpl()

private class LocalAiClientImpl : AiClient {
    private val generativeModel by lazy {
        Generation.getClient(GenerationConfig.Builder().build())
    }

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun request(
        messages: List<AiClient.GptMessage>,
        format: AiClient.Format,
        model: ChatGptModel,
    ): AiClient.GptResult {
        val textParts = mutableListOf<String>()
        var imagePart: ImagePart? = null

        for (message in messages) {
            for (content in message.contents) {
                when (content) {
                    is AiClient.GptMessage.Content.Text -> textParts.add(content.text)
                    is AiClient.GptMessage.Content.Base64Image -> {
                        if (imagePart == null) {
                            val bytes = Base64.decode(content.base64)
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            if (bitmap != null) {
                                imagePart = ImagePart(bitmap)
                            }
                        }
                    }
                    is AiClient.GptMessage.Content.ImageUrl -> Unit
                }
            }
        }

        val combinedText = textParts.joinToString("\n")
        val textPartObj = TextPart(combinedText)
        val request = if (imagePart != null) {
            GenerateContentRequest.Builder(imagePart, textPartObj).build()
        } else {
            GenerateContentRequest.Builder(textPartObj).build()
        }

        val response = generativeModel.generateContent(request)
        val text = response.candidates.firstOrNull()?.text ?: ""

        return AiClient.GptResult.Success(
            AiClient.AiResponse(
                choices = listOf(
                    AiClient.AiResponse.Choice(
                        message = AiClient.AiResponse.Choice.Message(
                            role = AiClient.AiResponse.Choice.Role.Assistant,
                            content = text,
                        ),
                    ),
                ),
            ),
        )
    }
}
