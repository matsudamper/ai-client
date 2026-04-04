package net.matsudamper.gptclient.worker

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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
    private val jobs = ConcurrentHashMap<String, Job>()

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
            ).run(
                chatRoomId = chatRoomId,
            )
        }
        jobs[workId] = job
        job.invokeOnCompletion {
            jobs.remove(workId)
        }
        return workId
    }

    override fun isWorkRunning(workId: String): Boolean {
        return jobs[workId]?.isActive == true
    }
}
