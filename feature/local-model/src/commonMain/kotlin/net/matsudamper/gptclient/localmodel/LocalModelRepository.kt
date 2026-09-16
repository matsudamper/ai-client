package net.matsudamper.gptclient.localmodel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface LocalModelRepository {
    suspend fun getModels(): List<LocalModelDefinition>
    suspend fun getResolvedModels(): List<LocalModelDefinition> = getModels()
    fun observeStatuses(): Flow<Map<LocalModelId, LocalModelState>>
    fun observeEngineLabels(): Flow<Map<LocalModelId, String>> = flowOf(emptyMap())
    suspend fun refreshStatuses()
    suspend fun enqueueDownload(modelId: LocalModelId)
    suspend fun delete(modelId: LocalModelId)
}

data class LocalModelState(
    val status: LocalModelStatus,
    val progress: Float? = null,
    val unavailableReason: String? = null,
)

enum class LocalModelStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    UNAVAILABLE,
}
