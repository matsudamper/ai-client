package net.matsudamper.gptclient.localmodel

import android.content.Context
import androidx.lifecycle.Observer
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.io.File
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import net.matsudamper.gptclient.client.local.LiteRtLmEngineStore

class LocalModelRepositoryImpl(
    private val context: Context,
    private val workManager: WorkManager,
) : LocalModelRepository {
    private val refreshTrigger = MutableStateFlow(0)

    override suspend fun getModels(): List<LocalModelDefinition> = AndroidLocalModels.entries

    override fun observeStatuses(): Flow<Map<String, LocalModelState>> {
        val workInfoFlows =
            AndroidLocalModels.entries.map { model ->
                observeWorkInfos(
                    LocalModelDownloadWorker.getUniqueWorkName(model.modelId),
                ).map { workInfos ->
                    model.modelId to workInfos
                }
            }

        return combine(refreshTrigger, combine(workInfoFlows) { it.toMap() }) { _, workInfoMap ->
            AndroidLocalModels.entries.associate { model ->
                model.modelId to createState(
                    modelId = model.modelId,
                    workInfos = workInfoMap[model.modelId].orEmpty(),
                )
            }
        }
    }

    override suspend fun enqueueDownload(modelId: String) {
        val model = AndroidLocalModels.find(modelId) ?: return
        val request =
            OneTimeWorkRequestBuilder<LocalModelDownloadWorker>()
                .setInputData(LocalModelDownloadWorker.createInputData(modelId))
                .build()
        workManager.enqueueUniqueWork(
            LocalModelDownloadWorker.getUniqueWorkName(model.modelId),
            ExistingWorkPolicy.KEEP,
            request,
        )
        refreshTrigger.update { it + 1 }
    }

    override suspend fun delete(modelId: String) {
        workManager.cancelUniqueWork(LocalModelDownloadWorker.getUniqueWorkName(modelId))
        LiteRtLmEngineStore.remove(modelId)
        getModelFile(context, modelId).delete()
        getTempModelFile(context, modelId).delete()
        refreshTrigger.update { it + 1 }
    }

    private fun createState(
        modelId: String,
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

        fun getModelFile(context: Context, modelId: String): File {
            val definition = requireNotNull(AndroidLocalModels.find(modelId)) {
                "Unknown modelId: $modelId"
            }
            return File(getModelsDirectory(context), definition.fileName)
        }

        fun getTempModelFile(context: Context, modelId: String): File =
            File(getModelsDirectory(context), "${getModelFile(context, modelId).name}.download")
    }
}
