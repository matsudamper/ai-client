package net.matsudamper.gptclient.localmodel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import net.matsudamper.gptclient.client.AiClient
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun localModelFeatureModule(): Module =
    module {
        single<LocalModelRepository> {
            JvmLocalModelRepository()
        }
        single<LocalModelAiClientFactory> {
            EmptyLocalModelAiClientFactory
        }
    }

internal class JvmLocalModelRepository : LocalModelRepository {
    override suspend fun getModels(): List<LocalModelDefinition> = emptyList()

    override fun observeStatuses(): Flow<Map<LocalModelId, LocalModelState>> = flowOf(emptyMap())

    override suspend fun enqueueDownload(modelId: LocalModelId) {}

    override suspend fun delete(modelId: LocalModelId) {}
}

internal object EmptyLocalModelAiClientFactory : LocalModelAiClientFactory {
    override fun create(modelId: LocalModelId): AiClient? = null
}
