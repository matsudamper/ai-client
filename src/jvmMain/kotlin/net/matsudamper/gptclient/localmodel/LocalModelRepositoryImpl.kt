package net.matsudamper.gptclient.localmodel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LocalModelRepositoryImpl : LocalModelRepository {
    override suspend fun getModels(): List<LocalModelDefinition> = emptyList()

    override suspend fun checkStatus(modelId: String): LocalModelStatus = LocalModelStatus.UNAVAILABLE

    override fun download(modelId: String): Flow<DownloadProgress> = flow {
        emit(DownloadProgress.Failed("ローカルモデルはこのプラットフォームではサポートされていません"))
    }
}
