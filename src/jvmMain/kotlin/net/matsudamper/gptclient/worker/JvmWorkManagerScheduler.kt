package net.matsudamper.gptclient.worker

import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.gptclient.PlatformRequest
import net.matsudamper.gptclient.datastore.SettingDataStore
import net.matsudamper.gptclient.localmodel.LocalModelAiClientFactory
import net.matsudamper.gptclient.localmodel.LocalModelRepository
import net.matsudamper.gptclient.room.AppDatabase
import net.matsudamper.gptclient.room.entity.ChatRoomId
import net.matsudamper.gptclient.viewmodel.AddRequestUseCase

class JvmWorkManagerScheduler(
    private val appDatabase: AppDatabase,
    private val platformRequest: PlatformRequest,
    private val settingDataStore: SettingDataStore,
    private val localModelRepository: LocalModelRepository,
    private val localModelAiClientFactory: LocalModelAiClientFactory,
) : AddRequestUseCase.WorkManagerScheduler {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val runningJobs = MutableStateFlow<Map<String, Job>>(mapOf())

    override fun scheduleWork(
        chatRoomId: ChatRoomId,
    ): String {
        val workId = UUID.randomUUID().toString()
        val job = scope.launch {
            ChatRequestRunner(
                appDatabase = appDatabase,
                platformRequest = platformRequest,
                settingDataStore = settingDataStore,
                localModelRepository = localModelRepository,
                localModelAiClientFactory = localModelAiClientFactory,
            ).run(chatRoomId = chatRoomId)
        }
        runningJobs.update { it.plus(workId to job) }
        job.invokeOnCompletion {
            runningJobs.update { it.minus(workId) }
        }
        return workId
    }

    override fun cancelWork(workId: String) {
        runningJobs.value[workId]?.cancel()
    }

    override fun hasWork(workId: String): Boolean {
        return runningJobs.value.containsKey(workId)
    }

    override fun observeWorkInProgress(workId: String): Flow<Boolean> {
        return runningJobs
            .map { it.containsKey(workId) }
            .distinctUntilChanged()
    }
}
