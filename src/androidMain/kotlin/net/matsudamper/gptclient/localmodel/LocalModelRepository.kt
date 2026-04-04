package net.matsudamper.gptclient.localmodel

import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

actual class LocalModelRepository actual constructor() {
    private fun getClient() = Generation.getClient()

    actual suspend fun getModels(): List<LocalModelDefinition> {
        return try {
            val client = getClient()
            val status = client.checkStatus()
            if (status == FeatureStatus.UNAVAILABLE) return emptyList()

            val baseName = try {
                client.getBaseModelName()
            } catch (e: Exception) {
                "Gemini Nano"
            }
            val tokenLimit = try {
                client.getTokenLimit()
            } catch (e: Exception) {
                1024
            }

            listOf(
                LocalModelDefinition(
                    modelId = "mlkit-prompt",
                    displayName = baseName,
                    description = "AI Coreオンデバイスモデル",
                    enableImage = true,
                    defaultToken = tokenLimit,
                ),
            )
        } catch (e: Exception) {
            listOf(
                LocalModelDefinition(
                    modelId = "mlkit-prompt",
                    displayName = "Gemini Nano",
                    description = "AI Coreオンデバイスモデル",
                    enableImage = true,
                    defaultToken = 1024,
                ),
            )
        }
    }

    actual suspend fun checkStatus(modelId: String): LocalModelStatus {
        return try {
            val client = getClient()
            when (client.checkStatus()) {
                FeatureStatus.AVAILABLE -> LocalModelStatus.AVAILABLE
                FeatureStatus.DOWNLOADABLE -> LocalModelStatus.DOWNLOADABLE
                FeatureStatus.DOWNLOADING -> LocalModelStatus.DOWNLOADING
                else -> LocalModelStatus.UNAVAILABLE
            }
        } catch (e: Exception) {
            LocalModelStatus.DOWNLOADABLE
        }
    }

    actual fun download(modelId: String): Flow<DownloadProgress> {
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
        } catch (e: Exception) {
            flow { emit(DownloadProgress.Failed(e.message ?: "ダウンロードを開始できません")) }
        }
    }
}
