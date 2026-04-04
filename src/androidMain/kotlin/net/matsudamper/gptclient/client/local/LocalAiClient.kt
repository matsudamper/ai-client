package net.matsudamper.gptclient.client.local

import android.graphics.BitmapFactory
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.ImagePart
import com.google.mlkit.genai.prompt.TextPart
import net.matsudamper.gptclient.client.AiClient
import net.matsudamper.gptclient.entity.ChatGptModel
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

actual fun createLocalAiClient(model: ChatGptModel.Local): AiClient? = MlKitAiClient()

private class MlKitAiClient : AiClient {
    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun request(
        messages: List<AiClient.GptMessage>,
        format: AiClient.Format,
        model: ChatGptModel,
    ): AiClient.GptResult {
        val client = Generation.getClient()

        val textParts = mutableListOf<String>()
        var firstImagePart: ImagePart? = null

        for (message in messages) {
            val rolePrefix = when (message.role) {
                AiClient.GptMessage.Role.System -> "[System] "
                AiClient.GptMessage.Role.User -> "[User] "
                AiClient.GptMessage.Role.Assistant -> "[Assistant] "
            }
            for (content in message.contents) {
                when (content) {
                    is AiClient.GptMessage.Content.Text -> {
                        textParts.add(rolePrefix + content.text)
                    }
                    is AiClient.GptMessage.Content.Base64Image -> {
                        if (firstImagePart == null) {
                            val bytes = Base64.decode(content.base64)
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            if (bitmap != null) {
                                firstImagePart = ImagePart(bitmap)
                            }
                        }
                    }
                    is AiClient.GptMessage.Content.ImageUrl -> Unit
                }
            }
        }

        val combinedText = textParts.joinToString("\n")
        val textPart = TextPart(combinedText)
        val request = if (firstImagePart != null) {
            GenerateContentRequest.Builder(firstImagePart, textPart).build()
        } else {
            GenerateContentRequest.Builder(textPart).build()
        }

        return try {
            val response = client.generateContent(request)
            val text = response.candidates.firstOrNull()?.text ?: ""
            client.close()

            AiClient.GptResult.Success(
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
        } catch (e: Exception) {
            client.close()
            AiClient.GptResult.Error(
                AiClient.GptResult.ErrorReason.Unknown(
                    e.message ?: "ローカルモデルでの推論に失敗しました",
                ),
            )
        }
    }
}
