package net.matsudamper.gptclient.client.local

import android.content.Context
import android.graphics.BitmapFactory
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.ImagePart
import com.google.mlkit.genai.prompt.TextPart
import net.matsudamper.gptclient.client.AiClient
import net.matsudamper.gptclient.entity.ChatGptModel
import net.matsudamper.gptclient.localmodel.LocalModelRepositoryImpl
import org.koin.java.KoinJavaComponent.getKoin
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import net.matsudamper.gptclient.util.Log

actual fun createLocalAiClient(model: ChatGptModel.Local): AiClient? {
    val repo: LocalModelRepositoryImpl = getKoin().get()
    Log.d("LOG", "model.modelKey=${model.modelKey}")
    // Determine backend from modelId prefix
    return if (model.modelKey == "mlkit-prompt") {
        MlKitAiClient()
    } else if (model.modelKey.startsWith("hf:")) {
        val context: Context = getKoin().get()
        val modelFile = LocalModelRepositoryImpl.getModelFile(context, model.modelKey)
        if (!modelFile.exists()) return null
        MediaPipeAiClient(modelFile.absolutePath, context)
    } else {
        null
    }
}

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
                    e.message ?: "ML Kitモデルでの推論に失敗しました",
                ),
            )
        }
    }
}

private class MediaPipeAiClient(
    private val modelPath: String,
    private val context: Context,
) : AiClient {
    override suspend fun request(
        messages: List<AiClient.GptMessage>,
        format: AiClient.Format,
        model: ChatGptModel,
    ): AiClient.GptResult {
        return try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(model.defaultToken)
                .build()

            val llmInference = LlmInference.createFromOptions(context, options)

            val textParts = mutableListOf<String>()
            for (message in messages) {
                val rolePrefix = when (message.role) {
                    AiClient.GptMessage.Role.System -> "[System] "
                    AiClient.GptMessage.Role.User -> "[User] "
                    AiClient.GptMessage.Role.Assistant -> "[Assistant] "
                }
                for (content in message.contents) {
                    if (content is AiClient.GptMessage.Content.Text) {
                        textParts.add(rolePrefix + content.text)
                    }
                }
            }

            val prompt = textParts.joinToString("\n")
            val response = llmInference.generateResponse(prompt)
            llmInference.close()

            AiClient.GptResult.Success(
                AiClient.AiResponse(
                    choices = listOf(
                        AiClient.AiResponse.Choice(
                            message = AiClient.AiResponse.Choice.Message(
                                role = AiClient.AiResponse.Choice.Role.Assistant,
                                content = response,
                            ),
                        ),
                    ),
                ),
            )
        } catch (e: Exception) {
            AiClient.GptResult.Error(
                AiClient.GptResult.ErrorReason.Unknown(
                    e.message ?: "MediaPipeモデルでの推論に失敗しました",
                ),
            )
        }
    }
}
