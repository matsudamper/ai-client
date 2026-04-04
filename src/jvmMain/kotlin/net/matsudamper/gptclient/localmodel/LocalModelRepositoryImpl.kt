package net.matsudamper.gptclient.localmodel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class LocalModelRepositoryImpl : LocalModelRepository {
    override suspend fun getModels(): List<LocalModelDefinition> = emptyList()

    override fun observeStatuses(): Flow<Map<LocalModelId, LocalModelState>> = flowOf(emptyMap())

    override suspend fun enqueueDownload(modelId: LocalModelId) {}

    override suspend fun delete(modelId: LocalModelId) {}
}
