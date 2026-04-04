package net.matsudamper.gptclient.localmodel

import android.content.Context
import net.matsudamper.gptclient.client.AiClient

internal class AndroidLocalModelAiClientFactory(
    private val context: Context,
) : LocalModelAiClientFactory {
    override fun create(modelId: LocalModelId): AiClient? {
        val modelDefinition = AndroidLocalModels.find(modelId) ?: return null
        return when (modelDefinition.providerId) {
            LocalModelProviderId.MlKitPrompt -> MlKitAiClient()
            LocalModelProviderId.LiteRtLm -> {
                val modelFile = LocalModelRepositoryImpl.getModelFile(context, modelId)
                if (!modelFile.exists()) return null

                LiteRtAiClient(
                    context = context,
                    modelDefinition = modelDefinition,
                    modelFile = modelFile,
                )
            }
        }
    }
}
