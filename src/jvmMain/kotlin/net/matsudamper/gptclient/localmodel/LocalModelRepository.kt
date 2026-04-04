package net.matsudamper.gptclient.localmodel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

actual class LocalModelRepository actual constructor() {
    actual suspend fun getModels(): List<LocalModelDefinition> = emptyList()

    actual suspend fun checkStatus(modelId: String): LocalModelStatus = LocalModelStatus.UNAVAILABLE

    actual fun download(modelId: String): Flow<DownloadProgress> = flow {
        emit(DownloadProgress.Failed("ローカルモデルはこのプラットフォームではサポートされていません"))
    }
}
