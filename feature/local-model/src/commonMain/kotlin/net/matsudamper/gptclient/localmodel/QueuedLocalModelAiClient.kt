package net.matsudamper.gptclient.localmodel

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
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
        LocalModelExecutionPhaseStore.update(modelId, LocalModelExecutionPhase.WaitingForOtherModel)
        return try {
            executionQueue.withExclusiveModel(modelId) {
                LocalModelExecutionPhaseStore.update(modelId, LocalModelExecutionPhase.LoadingModel)
                delegate.request(messages = messages, format = format)
            }
        } finally {
            withContext(NonCancellable) { LocalModelExecutionPhaseStore.clear(modelId) }
        }
    }
}
