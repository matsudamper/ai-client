package net.matsudamper.gptclient.localmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import net.matsudamper.gptclient.client.AiClient

internal class LiteRtAiClient(
    private val context: Context,
    private val modelDefinition: AndroidLocalModel,
    private val modelFile: File,
    private val enableThinking: Boolean,
) : AiClient {
    override suspend fun request(
        messages: List<AiClient.GptMessage>,
        format: AiClient.Format,
    ): AiClient.GptResult {
        return runCatching {
            val engine = LiteRtLmEngineStore.getOrCreate(context, modelDefinition, modelFile)
            val resolvedMessages = if (format != AiClient.Format.Text) {
                addJsonFormatInstruction(messages)
            } else {
                messages
            }
            require(resolvedMessages.isNotEmpty()) { "送信するメッセージがありません" }

            val (historySourceMessages, lastUserMessage) = resolvedMessages.prepareForLiteRt()
            val historyMessages = historySourceMessages.mapNotNull {
                it.toLiteRtMessage(includeImages = false)
            }
            val lastMessage = lastUserMessage.toLiteRtMessage(includeImages = modelDefinition.enableImage)
                ?: error("最後のメッセージが空です")

            engine.createConversation(
                ConversationConfig(
                    initialMessages = historyMessages,
                    samplerConfig = SamplerConfig(
                        topK = 5,
                        topP = 0.95,
                        temperature = 0.0,
                    ),
                ),
            ).use { conversation ->
                val responseMessage = conversation.sendMessage(
                    message = lastMessage,
                    extraContext = mapOf(
                        "enable_thinking" to enableThinking,
                    ),
                )
                responseMessage.extractText().stripMarkdownFence().toSuccessResult()
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
        val jsonInstruction = "応答はそのままJSONパーサに渡されます。マークダウンのコードブロック（```json, ``` など）を絶対に使用しないでください。有効なJSONオブジェクトのみを、追加テキスト無しで返してください。"
        return messages.indexOfFirst { it.role == AiClient.GptMessage.Role.System }
            .takeIf { it >= 0 }
            ?.let { systemIndex ->
                messages.mapIndexed { index, message ->
                    if (index == systemIndex) {
                        message.copy(
                            contents = message.contents + AiClient.GptMessage.Content.Text(jsonInstruction),
                        )
                    } else {
                        message
                    }
                }
            } ?: buildList {
            add(
                AiClient.GptMessage(
                    role = AiClient.GptMessage.Role.System,
                    contents = listOf(AiClient.GptMessage.Content.Text(jsonInstruction)),
                ),
            )
            addAll(messages)
        }
    }

    private fun List<AiClient.GptMessage>.prepareForLiteRt(): Pair<List<AiClient.GptMessage>, AiClient.GptMessage> {
        val trailingUserMessageCount = asReversed()
            .takeWhile { it.role == AiClient.GptMessage.Role.User }
            .count()
        require(trailingUserMessageCount > 0) { "ユーザーメッセージがありません" }

        val historySourceMessages = dropLast(trailingUserMessageCount)
        val trailingUserMessages = takeLast(trailingUserMessageCount)
        val maxImages = if (modelDefinition.enableImage) MAX_IMAGES_PER_TURN else 0
        val mergedLastUserMessage = mergeUserMessages(trailingUserMessages)
            .limitImages(maxImages = maxImages)

        return historySourceMessages to mergedLastUserMessage
    }

    private fun mergeUserMessages(messages: List<AiClient.GptMessage>): AiClient.GptMessage {
        return AiClient.GptMessage(
            role = AiClient.GptMessage.Role.User,
            contents = messages.flatMap { it.contents },
        )
    }

    private fun AiClient.GptMessage.limitImages(maxImages: Int): AiClient.GptMessage {
        var imageCount = 0
        val limitedContents = contents.filter { content ->
            when (content) {
                is AiClient.GptMessage.Content.Base64Image -> {
                    if (imageCount < maxImages) {
                        imageCount++
                        true
                    } else {
                        false
                    }
                }

                is AiClient.GptMessage.Content.ImageUrl,
                is AiClient.GptMessage.Content.Text,
                -> true
            }
        }
        return copy(contents = limitedContents)
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun AiClient.GptMessage.toLiteRtMessage(includeImages: Boolean = true): Message? {
        val liteRtContents = contents.mapNotNull { content ->
            when (content) {
                is AiClient.GptMessage.Content.Text -> Content.Text(content.text)
                is AiClient.GptMessage.Content.Base64Image ->
                    if (includeImages) {
                        content.toLiteRtImageBytes()?.let(Content::ImageBytes)
                    } else {
                        null
                    }

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
        return try {
            bitmap.toPngByteArray()
        } finally {
            bitmap.recycle()
        }
    }

    private fun Bitmap.toPngByteArray(): ByteArray {
        return ByteArrayOutputStream().use { outputStream ->
            compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, outputStream)
            outputStream.toByteArray()
        }
    }

    private fun Message.extractText(): String {
        return contents.contents
            .filterIsInstance<Content.Text>()
            .joinToString(separator = "") { it.text }
    }

    private fun String.stripMarkdownFence(): String {
        val trimmed = trim()
        val fenceRegex = Regex("^```(?:json5?)?\\s*\\n?([\\s\\S]*?)\\n?```\\s*$")
        return fenceRegex.find(trimmed)?.groupValues?.get(1)?.trim() ?: trimmed
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
        private const val MAX_IMAGES_PER_TURN = 1
    }
}
