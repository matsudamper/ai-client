package net.matsudamper.gptclient.worker

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.UUID
import net.matsudamper.gptclient.room.entity.ChatRoomId
import net.matsudamper.gptclient.viewmodel.AddRequestUseCase

class AndroidWorkManagerScheduler(
    private val workManager: WorkManager,
) : AddRequestUseCase.WorkManagerScheduler {

    override fun scheduleWork(
        chatRoomId: ChatRoomId,
    ): String {
        val inputData = ChatRequestWorker.createInputData(
            chatRoomId = chatRoomId,
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

    override fun cancelWork(workId: String) {
        workManager.cancelWorkById(UUID.fromString(workId))
    }

    override fun hasWork(workId: String): Boolean {
        return runCatching {
            workManager.getWorkInfoById(UUID.fromString(workId)).get() != null
        }.getOrDefault(false)
    }

    fun getChatRequestWorkerId(chatRoomId: ChatRoomId): String {
        return "chat_request_${chatRoomId.value}"
    }
}
