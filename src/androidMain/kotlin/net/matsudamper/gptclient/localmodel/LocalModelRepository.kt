package net.matsudamper.gptclient.localmodel

import android.content.Context
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.utils.io.readAvailable
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

actual class LocalModelRepository actual constructor() {
    private var context: Context? = null
    private val huggingFaceApi = HuggingFaceApi()

    fun setContext(context: Context) {
        this.context = context
    }

    actual suspend fun getModels(): List<LocalModelDefinition> {
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

        // HuggingFace models
        try {
            val hfModels = huggingFaceApi.listModels()
            for (hfModel in hfModels) {
                val shortName = hfModel.modelId.substringAfter("/")
                models.add(
                    LocalModelDefinition(
                        modelId = "hf:${hfModel.modelId}",
                        displayName = shortName,
                        description = "HuggingFace (MediaPipe)",
                        enableImage = false,
                        defaultToken = 1024,
                        backend = LocalModelDefinition.Backend.MEDIAPIPE,
                    ),
                )
            }
        } catch (_: Exception) {
            // API unreachable - show only already-downloaded HF models
            val ctx = context ?: return models
            val modelsDir = File(ctx.filesDir, "models")
            if (modelsDir.exists()) {
                modelsDir.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".task")) {
                        val modelId = file.nameWithoutExtension
                        models.add(
                            LocalModelDefinition(
                                modelId = "hf:$modelId",
                                displayName = modelId,
                                description = "HuggingFace (MediaPipe)",
                                enableImage = false,
                                defaultToken = 1024,
                                backend = LocalModelDefinition.Backend.MEDIAPIPE,
                            ),
                        )
                    }
                }
            }
        }

        return models
    }

    actual suspend fun checkStatus(modelId: String): LocalModelStatus {
        return when {
            modelId == "mlkit-prompt" -> checkMlKitStatus()
            modelId.startsWith("hf:") -> checkHfModelStatus(modelId)
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

    private fun checkHfModelStatus(modelId: String): LocalModelStatus {
        val ctx = context ?: return LocalModelStatus.DOWNLOADABLE
        val file = getModelFile(ctx, modelId)
        return if (file.exists()) LocalModelStatus.AVAILABLE else LocalModelStatus.DOWNLOADABLE
    }

    actual fun download(modelId: String): Flow<DownloadProgress> {
        return when {
            modelId == "mlkit-prompt" -> downloadMlKit()
            modelId.startsWith("hf:") -> downloadHfModel(modelId)
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

    private fun downloadHfModel(modelId: String): Flow<DownloadProgress> = flow {
        val ctx = context ?: run {
            emit(DownloadProgress.Failed("コンテキストが利用できません"))
            return@flow
        }
        val hfModelId = modelId.removePrefix("hf:")

        emit(DownloadProgress.Started)

        try {
            // Get file list and pick best .task file
            val siblings = huggingFaceApi.getModelFiles(hfModelId)
            val taskFile = HuggingFaceApi.pickBestTaskFile(siblings)
            if (taskFile == null) {
                emit(DownloadProgress.Failed("ダウンロード可能な.taskファイルが見つかりません"))
                return@flow
            }

            val downloadUrl = HuggingFaceApi.resolveDownloadUrl(hfModelId, taskFile)
            val modelFile = getModelFile(ctx, modelId)
            modelFile.parentFile?.mkdirs()
            val tempFile = File(modelFile.parent, "${modelFile.name}.tmp")

            withContext(Dispatchers.IO) {
                val httpClient = HttpClient()
                try {
                    val response = httpClient.get(downloadUrl)

                    val contentLength = response.bodyAsText().length.toLong()
                    // Use channel for streaming download
                    val channel = httpClient.get(downloadUrl).bodyAsChannel()
                    var downloadedBytes = 0L
                    val buffer = ByteArray(8192)

                    tempFile.outputStream().buffered().use { output ->
                        while (!channel.isClosedForRead) {
                            val read = channel.readAvailable(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            downloadedBytes += read
                            if (contentLength > 0) {
                                emit(DownloadProgress.InProgress(downloadedBytes.toFloat() / contentLength))
                            }
                        }
                    }
                    tempFile.renameTo(modelFile)
                } finally {
                    httpClient.close()
                }
            }

            emit(DownloadProgress.Completed)
        } catch (e: Exception) {
            emit(DownloadProgress.Failed(e.message ?: "ダウンロードに失敗しました"))
        }
    }

    companion object {
        fun getModelFile(context: Context, modelId: String): File {
            val safeName = modelId.replace(":", "_").replace("/", "_")
            return File(context.filesDir, "models/$safeName.task")
        }
    }
}
