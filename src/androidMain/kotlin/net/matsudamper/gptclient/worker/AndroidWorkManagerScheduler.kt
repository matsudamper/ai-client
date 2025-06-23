package net.matsudamper.gptclient.worker

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.UUID
import net.matsudamper.gptclient.room.entity.ChatRoomId
import net.matsudamper.gptclient.viewmodel.AddRequestUseCase

class AndroidWorkManagerScheduler(
    private val workManager: WorkManager,
) : AddRequestUseCase.WorkManagerScheduler {

    override fun scheduleWork(
        chatRoomId: ChatRoomId,
        message: String,
        uris: List<String>,
    ): String {
        val inputData = ChatRequestWorker.createInputData(
            chatRoomId = chatRoomId,
            message = message,
            uris = uris,
        )

        val workRequest = OneTimeWorkRequestBuilder<ChatRequestWorker>()
            .setInputData(inputData)
            .build()

        val workName = getChatRequestWorkerId(chatRoomId)
        workManager.enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            workRequest,
        )

        return workRequest.id.toString()
    }

    override fun isWorkRunning(workId: String): Boolean {
        return runCatching {
            val workInfo = workManager.getWorkInfoById(UUID.fromString(workId)).get()
            workInfo?.state == WorkInfo.State.RUNNING || workInfo?.state == WorkInfo.State.ENQUEUED
        }.isSuccess
    }

    fun getChatRequestWorkerId(chatRoomId: ChatRoomId): String {
        return "chat_request_${chatRoomId.value}"
    }
}
