package net.matsudamper.gptclient.client.local

import android.content.Context
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Message
import java.io.File
import net.matsudamper.gptclient.client.AiClient
import net.matsudamper.gptclient.entity.ChatGptModel
import net.matsudamper.gptclient.localmodel.AndroidLocalModels
import net.matsudamper.gptclient.localmodel.LocalModelDefinition
import net.matsudamper.gptclient.localmodel.LocalModelRepositoryImpl
import org.koin.java.KoinJavaComponent.getKoin
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

actual fun createLocalAiClient(model: ChatGptModel.Local): AiClient? {
    val context: Context = getKoin().get()
    val modelDefinition = AndroidLocalModels.find(model.modelKey) ?: return null
    val modelFile = LocalModelRepositoryImpl.getModelFile(context, model.modelKey)
    if (!modelFile.exists()) return null

    return LiteRtLmAiClient(
        context = context,
        modelDefinition = modelDefinition,
        modelFile = modelFile,
    )
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
