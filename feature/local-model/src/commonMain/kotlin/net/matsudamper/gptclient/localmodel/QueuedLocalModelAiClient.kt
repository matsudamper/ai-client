package net.matsudamper.gptclient.localmodel

import net.matsudamper.gptclient.client.AiClient

/**
 * ローカルモデルの推論をキュー経由で直列に実行する。
 */
internal class QueuedLocalModelAiClient(
    private val modelId: LocalModelId,
    private val executionQueue: LocalModelExecutionQueue,
    private val delegate: AiClient,
) : AiClient {
    override suspend fun request(
        messages: List<AiClient.GptMessage>,
        format: AiClient.Format,
    ): AiClient.GptResult {
        return executionQueue.withExclusiveModel(modelId) {
            delegate.request(messages = messages, format = format)
        }
    }
}
