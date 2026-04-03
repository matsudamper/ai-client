package net.matsudamper.gptclient.localmodel

import com.google.mlkit.genai.common.DownloadCallback
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerationConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

actual class LocalModelRepository actual constructor() {
    private val generativeModel by lazy {
        Generation.getClient(GenerationConfig.Builder().build())
    }

    actual suspend fun checkStatus(): LocalModelStatus {
        return try {
            when (generativeModel.checkStatus()) {
                FeatureStatus.DOWNLOADABLE -> LocalModelStatus.DOWNLOADABLE
                FeatureStatus.DOWNLOADING -> LocalModelStatus.DOWNLOADING
                FeatureStatus.AVAILABLE -> LocalModelStatus.AVAILABLE
                else -> LocalModelStatus.UNAVAILABLE
            }
        } catch (e: GenAiException) {
            // ErrorCode 606 (FEATURE_NOT_FOUND) はモデル未ダウンロード状態で発生しうる
            LocalModelStatus.DOWNLOADABLE
        }
    }

    actual fun download(): Flow<DownloadProgress> = callbackFlow {
        trySend(DownloadProgress.Started)
        generativeModel.download().collect { status ->
            when (status) {
                is DownloadStatus.DownloadStarted -> trySend(DownloadProgress.Started)
                is DownloadStatus.DownloadProgress -> trySend(DownloadProgress.InProgress(0f))
                is DownloadStatus.DownloadCompleted -> {
                    trySend(DownloadProgress.Completed)
                    close()
                }
                is DownloadStatus.DownloadFailed -> {
                    trySend(DownloadProgress.Failed("ダウンロードに失敗しました"))
                    close()
                }
            }
        }
        awaitClose()
    }
}
