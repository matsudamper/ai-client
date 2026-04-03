package net.matsudamper.gptclient.localmodel

import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.Generation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

actual class LocalModelRepository actual constructor() {
    private fun getClient() = Generation.getClient()

    actual suspend fun checkStatus(): LocalModelStatus {
        return try {
            val client = getClient()
            when (client.checkStatus()) {
                FeatureStatus.AVAILABLE -> LocalModelStatus.AVAILABLE
                FeatureStatus.DOWNLOADABLE -> LocalModelStatus.DOWNLOADABLE
                FeatureStatus.DOWNLOADING -> LocalModelStatus.DOWNLOADING
                else -> LocalModelStatus.UNAVAILABLE
            }
        } catch (e: GenAiException) {
            LocalModelStatus.DOWNLOADABLE
        }
    }

    actual fun download(): Flow<DownloadProgress> {
        return try {
            val client = getClient()
            var totalBytes = 0L
            client.download().map { status ->
                when (status) {
                    is DownloadStatus.DownloadStarted -> {
                        totalBytes = status.bytesToDownload
                        DownloadProgress.Started
                    }

                    is DownloadStatus.DownloadProgress -> {
                        val progress = if (totalBytes > 0) {
                            status.totalBytesDownloaded.toFloat() / totalBytes
                        } else {
                            0f
                        }
                        DownloadProgress.InProgress(progress)
                    }

                    is DownloadStatus.DownloadCompleted -> DownloadProgress.Completed
                    is DownloadStatus.DownloadFailed -> DownloadProgress.Failed(
                        status.e.message ?: "ダウンロードに失敗しました",
                    )

                    else -> DownloadProgress.Failed("不明なステータス")
                }
            }
        } catch (e: GenAiException) {
            flow { emit(DownloadProgress.Failed(e.message ?: "ダウンロードを開始できません")) }
        }
    }
}
