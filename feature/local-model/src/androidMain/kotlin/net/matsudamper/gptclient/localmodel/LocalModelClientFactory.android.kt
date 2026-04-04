package net.matsudamper.gptclient.localmodel

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
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

internal class AndroidLocalModelClientFactory(
    private val context: Context,
) : LocalModelClientFactory {
    override fun create(modelId: LocalModelId): LocalModelClient? {
        val modelDefinition = AndroidLocalModels.find(modelId) ?: return null
        return when (modelDefinition.providerId) {
            LocalModelProviderId.MlKitPrompt -> MlKitLocalModelClient()
            LocalModelProviderId.LiteRtLm -> {
                val modelFile = LocalModelRepositoryImpl.getModelFile(context, modelId)
                if (!modelFile.exists()) return null

                LiteRtLmLocalModelClient(
                    context = context,
                    modelDefinition = modelDefinition,
                    modelFile = modelFile,
                )
            }
        }
    }
}

private class LiteRtLmLocalModelClient(
    private val context: Context,
    private val modelDefinition: AndroidLocalModel,
    private val modelFile: File,
) : LocalModelClient {
    override suspend fun request(messages: List<LocalModelMessage>): LocalModelClientResult {
        return runCatching {
            val engine = LiteRtLmEngineStore.getOrCreate(context, modelDefinition, modelFile)
            val liteRtMessages = messages.mapNotNull { it.toLiteRtMessage() }
            require(liteRtMessages.isNotEmpty()) { "送信するメッセージがありません" }

            engine.createConversation(
                ConversationConfig(
                    initialMessages = liteRtMessages.dropLast(1),
                ),
            ).use { conversation ->
                val responseMessage = conversation.sendMessage(liteRtMessages.last())
                LocalModelClientResult.Success(responseMessage.toString())
            }
        }.getOrElse { throwable ->
            LocalModelClientResult.Error(
                throwable.message ?: "LiteRT-LM モデルでの推論に失敗しました",
            )
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun LocalModelMessage.toLiteRtMessage(): Message? {
        val liteRtContents =
            contents.mapNotNull { content ->
                when (content) {
                    is LocalModelMessage.Content.Text -> Content.Text(content.text)
                    is LocalModelMessage.Content.Base64Image ->
                        Content.ImageBytes(Base64.decode(content.base64))
                }
            }
        if (liteRtContents.isEmpty()) return null

        return when (role) {
            LocalModelMessage.Role.System -> Message.system(Contents.of(liteRtContents))
            LocalModelMessage.Role.User -> Message.user(Contents.of(liteRtContents))
            LocalModelMessage.Role.Assistant -> Message.model(Contents.of(liteRtContents))
        }
    }
}

private class MlKitLocalModelClient : LocalModelClient {
    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun request(messages: List<LocalModelMessage>): LocalModelClientResult {
        val client = Generation.getClient()

        return try {
            val textParts = mutableListOf<String>()
            var firstImagePart: ImagePart? = null

            for (message in messages) {
                val rolePrefix = when (message.role) {
                    LocalModelMessage.Role.System -> "[System] "
                    LocalModelMessage.Role.User -> "[User] "
                    LocalModelMessage.Role.Assistant -> "[Assistant] "
                }
                for (content in message.contents) {
                    when (content) {
                        is LocalModelMessage.Content.Text -> {
                            textParts.add(rolePrefix + content.text)
                        }

                        is LocalModelMessage.Content.Base64Image -> {
                            if (firstImagePart == null) {
                                val bytes = Base64.decode(content.base64)
                                val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                if (bitmap != null) {
                                    firstImagePart = ImagePart(bitmap)
                                }
                            }
                        }
                    }
                }
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
            LocalModelClientResult.Success(text)
        } catch (e: Exception) {
            LocalModelClientResult.Error(
                e.message ?: "ML Kitモデルでの推論に失敗しました",
            )
        } finally {
            client.close()
        }
    }
}
