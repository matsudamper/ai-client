package net.matsudamper.gptclient.localmodel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

actual class LocalModelRepository actual constructor() {
    actual suspend fun checkStatus(): LocalModelStatus = LocalModelStatus.UNAVAILABLE

    actual fun download(): Flow<DownloadProgress> = flow {
        emit(DownloadProgress.Failed("ローカルモデルはこのプラットフォームではサポートされていません"))
    }
}
