package net.matsudamper.gptclient.worker

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import net.matsudamper.gptclient.room.entity.ChatRoomId
import net.matsudamper.gptclient.viewmodel.AddRequestUseCase

class AndroidWorkManagerScheduler(
    private val workManager: WorkManager,
) : AddRequestUseCase.WorkManagerScheduler {

    override suspend fun scheduleWork(
        chatRoomId: ChatRoomId,
    ): String {
        val inputData = ChatRequestWorker.createInputData(
            chatRoomId = chatRoomId,
        )

        val workRequest = OneTimeWorkRequestBuilder<ChatRequestWorker>()
            .setInputData(inputData)
            .build()

        val workName = getChatRequestWorkerId(chatRoomId)
        // WorkInfoが登録される前にhasWork/observeWorkInProgressが実行中でないと誤判定するため、登録完了まで待つ
        workManager.enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            workRequest,
        ).result.get()

        return workRequest.id.toString()
    }

    override fun cancelWork(workId: String) {
        workManager.cancelWorkById(UUID.fromString(workId))
    }

    override fun hasWork(workId: String): Boolean {
        return runCatching {
            workManager.getWorkInfoById(UUID.fromString(workId)).get()?.state?.isFinished == false
        }.getOrDefault(false)
    }

    override fun observeWorkInProgress(workId: String): Flow<Boolean> {
        return workManager.getWorkInfoByIdFlow(UUID.fromString(workId))
            .map { workInfo -> workInfo?.state?.isFinished == false }
            .distinctUntilChanged()
    }

    fun getChatRequestWorkerId(chatRoomId: ChatRoomId): String {
        return "chat_request_${chatRoomId.value}"
    }
}
