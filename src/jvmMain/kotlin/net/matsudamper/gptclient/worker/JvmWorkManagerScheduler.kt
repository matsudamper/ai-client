package net.matsudamper.gptclient.worker

import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
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
    private val runningWorks = MutableStateFlow<Map<String, RunningWork>>(mapOf())

    override suspend fun scheduleWork(
        chatRoomId: ChatRoomId,
    ): String {
        cancelWorkOf(chatRoomId = chatRoomId)

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
        runningWorks.update { it.plus(workId to RunningWork(chatRoomId = chatRoomId, job = job)) }
        job.invokeOnCompletion {
            runningWorks.update { it.minus(workId) }
        }
        return workId
    }

    override fun cancelWork(workId: String) {
        runningWorks.value[workId]?.job?.cancel()
    }

    override fun hasWork(workId: String): Boolean {
        return runningWorks.value.containsKey(workId)
    }

    override fun observeWorkInProgress(workId: String): Flow<Boolean> {
        return runningWorks
            .map { it.containsKey(workId) }
            .distinctUntilChanged()
    }

    /**
     * Android の enqueueUniqueWork(REPLACE) と同じく、同一ルームの実行は常に一つに保つ。
     * 新しい実行と書き込みが重ならないよう、終了まで待ってから戻る。
     */
    private suspend fun cancelWorkOf(chatRoomId: ChatRoomId) {
        runningWorks.value.values
            .filter { it.chatRoomId == chatRoomId }
            .forEach { it.job.cancelAndJoin() }
    }

    private data class RunningWork(
        val chatRoomId: ChatRoomId,
        val job: Job,
    )
}
