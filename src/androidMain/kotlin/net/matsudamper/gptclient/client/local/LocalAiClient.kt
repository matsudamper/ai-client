package net.matsudamper.gptclient.client.local

import android.content.Context
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Message
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.ImagePart
import com.google.mlkit.genai.prompt.TextPart
import java.io.File
import net.matsudamper.gptclient.client.AiClient
import net.matsudamper.gptclient.entity.ChatGptModel
import net.matsudamper.gptclient.localmodel.AndroidLocalModels
import net.matsudamper.gptclient.localmodel.LocalModelDefinition
import net.matsudamper.gptclient.localmodel.LocalModelId
import net.matsudamper.gptclient.localmodel.LocalModelProviderIds
import net.matsudamper.gptclient.localmodel.LocalModelRepositoryImpl
import org.koin.java.KoinJavaComponent.getKoin
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

actual fun createLocalAiClient(model: ChatGptModel.Local): AiClient? {
    val context: Context = getKoin().get()
    val modelId = LocalModelId(model.modelKey)
    val modelDefinition = AndroidLocalModels.find(modelId) ?: return null
    return when (modelDefinition.providerId) {
        LocalModelProviderIds.MlKitPrompt -> MlKitAiClient()
        LocalModelProviderIds.LiteRtLm -> {
            val modelFile = LocalModelRepositoryImpl.getModelFile(context, modelId)
            if (!modelFile.exists()) return null

            LiteRtLmAiClient(
                context = context,
                modelDefinition = modelDefinition,
                modelFile = modelFile,
            )
        }

        else -> null
    }
}

private class LiteRtLmAiClient(
    private val context: Context,
    private val modelDefinition: LocalModelDefinition,
    private val modelFile: File,
) : AiClient {
    override suspend fun request(
        messages: List<AiClient.GptMessage>,
        format: AiClient.Format,
        model: ChatGptModel,
    ): AiClient.GptResult {
        return runCatching {
            val engine = LiteRtLmEngineStore.getOrCreate(context, modelDefinition, modelFile)
            val liteRtMessages = messages.mapNotNull { it.toLiteRtMessage() }
            require(liteRtMessages.isNotEmpty()) { "送信するメッセージがありません" }

            val responseMessage =
                engine.createConversation(
                    ConversationConfig(
                        initialMessages = liteRtMessages.dropLast(1),
                    ),
                ).use { conversation ->
                    conversation.sendMessage(liteRtMessages.last())
                }

            AiClient.GptResult.Success(
                AiClient.AiResponse(
                    choices = listOf(
                        AiClient.AiResponse.Choice(
                            message = AiClient.AiResponse.Choice.Message(
                                role = AiClient.AiResponse.Choice.Role.Assistant,
                                content = responseMessage.toString(),
                            ),
                        ),
                    ),
                ),
            )
        }.getOrElse { throwable ->
            AiClient.GptResult.Error(
                AiClient.GptResult.ErrorReason.Unknown(
                    throwable.message ?: "LiteRT-LM モデルでの推論に失敗しました",
                ),
            )
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun AiClient.GptMessage.toLiteRtMessage(): Message? {
        val liteRtContents =
            contents.mapNotNull { content ->
                when (content) {
                    is AiClient.GptMessage.Content.Text -> Content.Text(content.text)
                    is AiClient.GptMessage.Content.Base64Image ->
                        Content.ImageBytes(Base64.decode(content.base64))

                    is AiClient.GptMessage.Content.ImageUrl -> null
                }
            }
        if (liteRtContents.isEmpty()) return null

        return when (role) {
            AiClient.GptMessage.Role.System -> Message.system(Contents.of(liteRtContents))
            AiClient.GptMessage.Role.User -> Message.user(Contents.of(liteRtContents))
            AiClient.GptMessage.Role.Assistant -> Message.model(Contents.of(liteRtContents))
        }
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
                                val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
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
