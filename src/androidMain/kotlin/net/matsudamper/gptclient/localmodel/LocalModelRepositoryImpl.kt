package net.matsudamper.gptclient.localmodel

import android.content.Context
import androidx.lifecycle.Observer
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.gptclient.client.local.LiteRtLmEngineStore

class LocalModelRepositoryImpl(
    private val context: Context,
    private val workManager: WorkManager,
) : LocalModelRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshTrigger = MutableStateFlow(0)
    private val mlKitStatuses = MutableStateFlow<Map<LocalModelId, LocalModelState>>(emptyMap())

    init {
        scope.launch {
            refreshMlKitStatuses()
        }
    }

    override suspend fun getModels(): List<LocalModelDefinition> = AndroidLocalModels.entries

    override fun observeStatuses(): Flow<Map<LocalModelId, LocalModelState>> {
        val liteRtWorkInfoFlows =
            AndroidLocalModels.entries
                .filter { it.providerId == LocalModelProviderIds.LiteRtLm }
                .map { model ->
                    observeWorkInfos(
                        LocalModelDownloadWorker.getUniqueWorkName(model.modelId),
                    ).map { workInfos ->
                        model.modelId to workInfos
                    }
                }

        val liteRtStatusesFlow =
            if (liteRtWorkInfoFlows.isEmpty()) {
                MutableStateFlow<Map<LocalModelId, List<WorkInfo>>>(emptyMap())
            } else {
                combine(liteRtWorkInfoFlows) { it.toMap() }
            }

        return combine(
            refreshTrigger,
            mlKitStatuses,
            liteRtStatusesFlow,
        ) { _, mlKitStateMap, liteRtWorkInfoMap ->
            AndroidLocalModels.entries.associate { model ->
                val state =
                    when (model.providerId) {
                        LocalModelProviderIds.MlKitPrompt ->
                            mlKitStateMap[model.modelId] ?: LocalModelState(LocalModelStatus.UNAVAILABLE)

                        LocalModelProviderIds.LiteRtLm ->
                            createLiteRtState(
                                modelId = model.modelId,
                                workInfos = liteRtWorkInfoMap[model.modelId].orEmpty(),
                            )

                        else -> LocalModelState(LocalModelStatus.UNAVAILABLE)
                    }
                model.modelId to state
            }
        }
    }

    override suspend fun enqueueDownload(modelId: LocalModelId) {
        val model = AndroidLocalModels.find(modelId) ?: return
        when (model.providerId) {
            LocalModelProviderIds.MlKitPrompt -> downloadMlKitModel(model)
            LocalModelProviderIds.LiteRtLm -> enqueueLiteRtModel(model)
        }
    }

    override suspend fun delete(modelId: LocalModelId) {
        val model = AndroidLocalModels.find(modelId) ?: return
        when (model.providerId) {
            LocalModelProviderIds.MlKitPrompt -> Unit
            LocalModelProviderIds.LiteRtLm -> {
                workManager.cancelUniqueWork(LocalModelDownloadWorker.getUniqueWorkName(modelId))
                LiteRtLmEngineStore.remove(modelId)
                getModelFile(context, modelId).delete()
                getTempModelFile(context, modelId).delete()
                refreshTrigger.update { it + 1 }
            }
        }
    }

    private suspend fun refreshMlKitStatuses() {
        val states =
            AndroidLocalModels.entries
                .filter { it.providerId == LocalModelProviderIds.MlKitPrompt }
                .associate { model ->
                    model.modelId to checkMlKitStatus()
                }
        mlKitStatuses.value = states
    }

    private suspend fun checkMlKitStatus(): LocalModelState {
        return try {
            val client = Generation.getClient()
            val status =
                when (client.checkStatus()) {
                    FeatureStatus.AVAILABLE -> LocalModelStatus.DOWNLOADED
                    FeatureStatus.DOWNLOADABLE -> LocalModelStatus.NOT_DOWNLOADED
                    FeatureStatus.DOWNLOADING -> LocalModelStatus.DOWNLOADING
                    else -> LocalModelStatus.UNAVAILABLE
                }
            client.close()
            LocalModelState(status = status)
        } catch (_: Exception) {
            LocalModelState(status = LocalModelStatus.UNAVAILABLE)
        }
    }

    private suspend fun downloadMlKitModel(model: LocalModelDefinition) {
        val client = try {
            Generation.getClient()
        } catch (_: Exception) {
            mlKitStatuses.update { it + (model.modelId to LocalModelState(LocalModelStatus.UNAVAILABLE)) }
            return
        }

        var totalBytes = 0L
        try {
            client.download().collect { status ->
                when (status) {
                    is DownloadStatus.DownloadStarted -> {
                        totalBytes = status.bytesToDownload
                        mlKitStatuses.update {
                            it + (model.modelId to LocalModelState(LocalModelStatus.DOWNLOADING))
                        }
                    }

                    is DownloadStatus.DownloadProgress -> {
                        val progress =
                            if (totalBytes > 0) {
                                status.totalBytesDownloaded.toFloat() / totalBytes
                            } else {
                                null
                            }
                        mlKitStatuses.update {
                            it + (
                                model.modelId to LocalModelState(
                                    status = LocalModelStatus.DOWNLOADING,
                                    progress = progress,
                                )
                            )
                        }
                    }

                    is DownloadStatus.DownloadCompleted -> {
                        mlKitStatuses.update {
                            it + (model.modelId to LocalModelState(LocalModelStatus.DOWNLOADED))
                        }
                    }

                    is DownloadStatus.DownloadFailed -> {
                        mlKitStatuses.update {
                            it + (model.modelId to LocalModelState(LocalModelStatus.NOT_DOWNLOADED))
                        }
                    }
                }
            }
        } catch (_: Exception) {
            mlKitStatuses.update {
                it + (model.modelId to LocalModelState(LocalModelStatus.NOT_DOWNLOADED))
            }
        } finally {
            client.close()
        }
    }

    private fun enqueueLiteRtModel(model: LocalModelDefinition) {
        val request =
            OneTimeWorkRequestBuilder<LocalModelDownloadWorker>()
                .setInputData(LocalModelDownloadWorker.createInputData(model.modelId))
                .build()
        workManager.enqueueUniqueWork(
            LocalModelDownloadWorker.getUniqueWorkName(model.modelId),
            ExistingWorkPolicy.KEEP,
            request,
        )
        refreshTrigger.update { it + 1 }
    }

    private fun createLiteRtState(
        modelId: LocalModelId,
        workInfos: List<WorkInfo>,
    ): LocalModelState {
        if (getModelFile(context, modelId).exists()) {
            return LocalModelState(status = LocalModelStatus.DOWNLOADED)
        }

        val runningWorkInfo =
            workInfos.firstOrNull { workInfo ->
                workInfo.state == WorkInfo.State.RUNNING ||
                    workInfo.state == WorkInfo.State.ENQUEUED ||
                    workInfo.state == WorkInfo.State.BLOCKED
            }
        if (runningWorkInfo != null) {
            return LocalModelState(
                status = LocalModelStatus.DOWNLOADING,
                progress = LocalModelDownloadWorker.getProgress(runningWorkInfo.progress),
            )
        }

        return LocalModelState(status = LocalModelStatus.NOT_DOWNLOADED)
    }

    private fun observeWorkInfos(
        uniqueWorkName: String,
    ): Flow<List<WorkInfo>> = callbackFlow {
        val liveData = workManager.getWorkInfosForUniqueWorkLiveData(uniqueWorkName)
        val observer = Observer<List<WorkInfo>> { workInfos ->
            trySend(workInfos.orEmpty())
        }
        liveData.observeForever(observer)
        awaitClose {
            liveData.removeObserver(observer)
        }
    }

    companion object {
        fun getModelsDirectory(context: Context): File =
            File(context.filesDir, "models").apply {
                mkdirs()
            }

        fun getModelFile(context: Context, modelId: LocalModelId): File {
            val definition = requireNotNull(AndroidLocalModels.find(modelId)) {
                "Unknown modelId: ${modelId.value}"
            }
            val fileName = requireNotNull(definition.fileName) {
                "Model does not use a local file: ${modelId.value}"
            }
            return File(getModelsDirectory(context), fileName)
        }

        fun getTempModelFile(context: Context, modelId: LocalModelId): File =
            File(getModelsDirectory(context), "${getModelFile(context, modelId).name}.download")
    }
}
