package net.matsudamper.gptclient.localmodel

import android.content.Context
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation

class LocalModelRepositoryImpl: LocalModelRepository {

    override suspend fun getModels(): List<LocalModelDefinition> {
        val models = mutableListOf<LocalModelDefinition>()

        // ML Kit Prompt model
        val mlKitModel = try {
            val client = Generation.getClient()
            val name = try { client.getBaseModelName() } catch (_: Exception) { "Gemini Nano" }
            val tokenLimit = try { client.getTokenLimit() } catch (_: Exception) { 1024 }
            LocalModelDefinition(
                modelId = "mlkit-prompt",
                displayName = name,
                description = "ML Kit (AI Core)",
                enableImage = true,
                defaultToken = tokenLimit,
                backend = LocalModelDefinition.Backend.ML_KIT,
            )
        } catch (_: Exception) {
            LocalModelDefinition(
                modelId = "mlkit-prompt",
                displayName = "Gemini Nano",
                description = "ML Kit (AI Core)",
                enableImage = true,
                defaultToken = 1024,
                backend = LocalModelDefinition.Backend.ML_KIT,
            )
        }
        models.add(mlKitModel)

        return models
    }

    override suspend fun checkStatus(modelId: String): LocalModelStatus {
        return when {
            modelId == "mlkit-prompt" -> checkMlKitStatus()
            else -> LocalModelStatus.UNAVAILABLE
        }
    }

    private suspend fun checkMlKitStatus(): LocalModelStatus {
        return try {
            val client = Generation.getClient()
            when (client.checkStatus()) {
                FeatureStatus.AVAILABLE -> LocalModelStatus.AVAILABLE
                FeatureStatus.DOWNLOADABLE -> LocalModelStatus.DOWNLOADABLE
                FeatureStatus.DOWNLOADING -> LocalModelStatus.DOWNLOADING
                else -> LocalModelStatus.UNAVAILABLE
            }
        } catch (_: Exception) {
            LocalModelStatus.UNAVAILABLE
        }
    }

    override fun download(modelId: String): Flow<DownloadProgress> {
        return when {
            modelId == "mlkit-prompt" -> downloadMlKit()
            else -> flow { emit(DownloadProgress.Failed("不明なモデル")) }
        }
    }

    private fun downloadMlKit(): Flow<DownloadProgress> {
        return try {
            val client = Generation.getClient()
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

    companion object {
        fun getModelFile(context: Context, modelId: String): File {
            val safeName = modelId.replace(":", "_").replace("/", "_")
            return File(context.filesDir, "models/$safeName.task")
        }
    }
}
