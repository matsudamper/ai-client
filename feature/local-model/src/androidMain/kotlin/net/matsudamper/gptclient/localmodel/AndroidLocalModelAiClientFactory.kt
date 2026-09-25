package net.matsudamper.gptclient.localmodel

import android.content.Context
import net.matsudamper.gptclient.client.AiClient

internal class AndroidLocalModelAiClientFactory(
    private val context: Context,
    private val executionQueue: LocalModelExecutionQueue,
) : LocalModelAiClientFactory {
    override fun create(modelId: LocalModelId, enableThinking: Boolean): AiClient? {
        val client = createClient(modelId = modelId, enableThinking = enableThinking) ?: return null
        return QueuedLocalModelAiClient(
            modelId = modelId,
            executionQueue = executionQueue,
            delegate = client,
        )
    }

    private fun createClient(modelId: LocalModelId, enableThinking: Boolean): AiClient? {
        val modelDefinition = AndroidLocalModels.find(modelId) ?: return null
        return when (modelDefinition.providerId) {
            LocalModelProviderId.MlKitPrompt ->
                MlKitAiClient(
                    modelId = modelId,
                    generationConfig = modelDefinition.createMlKitGenerationConfig(),
                )

            LocalModelProviderId.LiteRtLm -> {
                val modelFile = LocalModelRepositoryImpl.getModelFile(context, modelId)
                if (!modelFile.exists()) return null

                LiteRtAiClient(
                    context = context,
                    modelDefinition = modelDefinition,
                    modelFile = modelFile,
                    enableThinking = enableThinking && modelDefinition.supportsThinking,
                )
            }
        }
    }
}
