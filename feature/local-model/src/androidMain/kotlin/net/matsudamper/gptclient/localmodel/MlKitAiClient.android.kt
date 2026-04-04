package net.matsudamper.gptclient.localmodel

import android.graphics.BitmapFactory
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.ImagePart
import com.google.mlkit.genai.prompt.TextPart
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import net.matsudamper.gptclient.client.AiClient

internal class MlKitAiClient : AiClient {
    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun request(
        messages: List<AiClient.GptMessage>,
        format: AiClient.Format,
    ): AiClient.GptResult {
        val client = Generation.getClient()

        return try {
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

            if (format == AiClient.Format.Json) {
                textParts.add(0, "[System] 応答はそのままJSONパーサに渡されます。マークダウンのコードブロックや余分なテキストを含めず、有効なJSONのみを返してください。")
            }
            val combinedText = textParts.joinToString("\n")
            val textPart = TextPart(combinedText)
            val request =
                if (firstImagePart != null) {
                    GenerateContentRequest.Builder(firstImagePart, textPart).build()
                } else {
                    GenerateContentRequest.Builder(textPart).build()
                }

            val response = client.generateContent(request)
            val text = response.candidates.firstOrNull()?.text.orEmpty()
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
            AiClient.GptResult.Error(
                AiClient.GptResult.ErrorReason.Unknown(
                    e.message ?: "ML Kitモデルでの推論に失敗しました",
                ),
            )
        } finally {
            client.close()
        }
    }
}
