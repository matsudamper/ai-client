package net.matsudamper.gptclient.localmodel

import android.content.Context
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

actual class LocalModelRepository actual constructor() {
    private var context: Context? = null

    fun setContext(context: Context) {
        this.context = context
    }

    actual fun getModels(): List<LocalModelDefinition> = getAvailableLocalModels()

    actual suspend fun checkStatus(modelId: String): LocalModelStatus {
        val model = getAvailableLocalModels().find { it.modelId == modelId }
            ?: return LocalModelStatus.UNAVAILABLE

        return when (model.backend) {
            LocalModelDefinition.Backend.ML_KIT -> checkMlKitStatus()
            LocalModelDefinition.Backend.MEDIAPIPE -> checkMediaPipeStatus(modelId)
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
        } catch (e: Exception) {
            LocalModelStatus.DOWNLOADABLE
        }
    }

    private fun checkMediaPipeStatus(modelId: String): LocalModelStatus {
        val ctx = context ?: return LocalModelStatus.DOWNLOADABLE
        val modelFile = getModelFile(ctx, modelId)
        return if (modelFile.exists()) {
            LocalModelStatus.AVAILABLE
        } else {
            LocalModelStatus.DOWNLOADABLE
        }
    }

    actual fun download(modelId: String): Flow<DownloadProgress> {
        val model = getAvailableLocalModels().find { it.modelId == modelId }
            ?: return flow { emit(DownloadProgress.Failed("モデルが見つかりません")) }

        return when (model.backend) {
            LocalModelDefinition.Backend.ML_KIT -> downloadMlKit()
            LocalModelDefinition.Backend.MEDIAPIPE -> downloadMediaPipe(modelId)
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

    private fun downloadMediaPipe(modelId: String): Flow<DownloadProgress> = flow {
        val ctx = context ?: run {
            emit(DownloadProgress.Failed("コンテキストが利用できません"))
            return@flow
        }
        val downloadUrl = getDownloadUrl(modelId) ?: run {
            emit(DownloadProgress.Failed("ダウンロードURLが不明です"))
            return@flow
        }

        emit(DownloadProgress.Started)

        try {
            withContext(Dispatchers.IO) {
                val modelFile = getModelFile(ctx, modelId)
                modelFile.parentFile?.mkdirs()
                val tempFile = File(modelFile.parent, "${modelFile.name}.tmp")

                val url = URL(downloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 30_000
                connection.readTimeout = 30_000

                try {
                    val totalBytes = connection.contentLengthLong
                    var downloadedBytes = 0L

                    connection.inputStream.buffered().use { input ->
                        tempFile.outputStream().buffered().use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                downloadedBytes += bytesRead
                                if (totalBytes > 0) {
                                    emit(DownloadProgress.InProgress(downloadedBytes.toFloat() / totalBytes))
                                }
                            }
                        }
                    }

                    tempFile.renameTo(modelFile)
                } finally {
                    connection.disconnect()
                }
            }

            emit(DownloadProgress.Completed)
        } catch (e: Exception) {
            emit(DownloadProgress.Failed(e.message ?: "ダウンロードに失敗しました"))
        }
    }

    companion object {
        fun getModelFile(context: Context, modelId: String): File {
            return File(context.filesDir, "models/$modelId.bin")
        }

        private fun getDownloadUrl(modelId: String): String? = when (modelId) {
            "local-gemma-3n-e2b" ->
                "https://huggingface.co/litert-community/Gemma3n-E2B-it/resolve/main/gemma3n_e2b_it.task"
            "local-gemma-2-2b" ->
                "https://huggingface.co/litert-community/Gemma-2-2B-it/resolve/main/gemma2_2b_it_gpu_int8.bin"
            else -> null
        }
    }
}
