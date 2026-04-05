package net.matsudamper.gptclient.localmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Message
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import net.matsudamper.gptclient.client.AiClient

internal class LiteRtAiClient(
    private val context: Context,
    private val modelDefinition: AndroidLocalModel,
    private val modelFile: File,
) : AiClient {
    override suspend fun request(
        messages: List<AiClient.GptMessage>,
        format: AiClient.Format,
    ): AiClient.GptResult {
        return runCatching {
            val engine = LiteRtLmEngineStore.getOrCreate(context, modelDefinition, modelFile)
            val resolvedMessages = if (format == AiClient.Format.Json) {
                addJsonFormatInstruction(messages)
            } else {
                messages
            }
            require(resolvedMessages.isNotEmpty()) { "送信するメッセージがありません" }

            val historyMessages = resolvedMessages.dropLast(1).mapNotNull { it.toLiteRtMessage(includeImages = false) }
            val lastMessage = resolvedMessages.last().toLiteRtMessage(includeImages = true)
                ?: error("最後のメッセージが空です")

            engine.createConversation(
                ConversationConfig(
                    initialMessages = historyMessages,
                ),
            ).use { conversation ->
                val responseMessage = conversation.sendMessage(
                    message = lastMessage,
                    extraContext = mapOf(
                        "enable_thinking" to true,
                    ),
                )
                responseMessage.toString().toSuccessResult()
            }
        }.getOrElse { throwable ->
            AiClient.GptResult.Error(
                AiClient.GptResult.ErrorReason.Unknown(
                    throwable.message ?: "LiteRT-LM モデルでの推論に失敗しました",
                ),
            )
        }
    }

    private fun addJsonFormatInstruction(messages: List<AiClient.GptMessage>): List<AiClient.GptMessage> {
        val jsonInstruction = "応答はそのままJSONパーサに渡されます。マークダウンのコードブロックや余分なテキストを含めず、有効なJSONのみを返してください。"
        val systemIndex = messages.indexOfFirst { it.role == AiClient.GptMessage.Role.System }
        return if (systemIndex >= 0) {
            messages.toMutableList().also { list ->
                val existing = list[systemIndex]
                list[systemIndex] = existing.copy(
                    contents = existing.contents + AiClient.GptMessage.Content.Text(jsonInstruction),
                )
            }
        } else {
            listOf(
                AiClient.GptMessage(
                    role = AiClient.GptMessage.Role.System,
                    contents = listOf(AiClient.GptMessage.Content.Text(jsonInstruction)),
                ),
            ) + messages
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun AiClient.GptMessage.toLiteRtMessage(includeImages: Boolean = true): Message? {
        val liteRtContents = contents.mapNotNull { content ->
            when (content) {
                is AiClient.GptMessage.Content.Text -> Content.Text(content.text)
                is AiClient.GptMessage.Content.Base64Image ->
                    if (includeImages) content.toLiteRtImageBytes()?.let(Content::ImageBytes)
                    else null

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

    @OptIn(ExperimentalEncodingApi::class)
    private fun AiClient.GptMessage.Content.Base64Image.toLiteRtImageBytes(): ByteArray? {
        val imageBytes = Base64.decode(base64)
        if (mimeType == PNG_MIME_TYPE) {
            return imageBytes
        }

        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return null

        return bitmap.toPngByteArray()
    }

    private fun Bitmap.toPngByteArray(): ByteArray {
        return ByteArrayOutputStream().use { outputStream ->
            compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, outputStream)
            outputStream.toByteArray()
        }
    }

    private fun String.toSuccessResult(): AiClient.GptResult.Success {
        return AiClient.GptResult.Success(
            AiClient.AiResponse(
                choices = listOf(
                    AiClient.AiResponse.Choice(
                        message = AiClient.AiResponse.Choice.Message(
                            role = AiClient.AiResponse.Choice.Role.Assistant,
                            content = this,
                        ),
                    ),
                ),
            ),
        )
    }

    private companion object {
        private const val PNG_QUALITY = 100
        private const val PNG_MIME_TYPE = "image/png"
    }
}
