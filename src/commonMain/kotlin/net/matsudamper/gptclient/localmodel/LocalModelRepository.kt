package net.matsudamper.gptclient.localmodel

import kotlinx.coroutines.flow.Flow

interface LocalModelRepository {
    suspend fun getModels(): List<LocalModelDefinition>
    fun observeStatuses(): Flow<Map<String, LocalModelState>>
    suspend fun enqueueDownload(modelId: String)
    suspend fun delete(modelId: String)
}

data class LocalModelState(
    val status: LocalModelStatus,
    val progress: Float? = null,
)

enum class LocalModelStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
}
