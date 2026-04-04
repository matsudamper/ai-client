package net.matsudamper.gptclient.localmodel

import kotlinx.coroutines.flow.Flow

expect class LocalModelRepository() {
    suspend fun getModels(): List<LocalModelDefinition>
    suspend fun checkStatus(modelId: String): LocalModelStatus
    fun download(modelId: String): Flow<DownloadProgress>
}

enum class LocalModelStatus {
    AVAILABLE,
    DOWNLOADABLE,
    DOWNLOADING,
    UNAVAILABLE,
}

sealed interface DownloadProgress {
    data object Started : DownloadProgress
    data class InProgress(val progress: Float) : DownloadProgress
    data object Completed : DownloadProgress
    data class Failed(val message: String) : DownloadProgress
}
