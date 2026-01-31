package net.matsudamper.gptclient.worker

import net.matsudamper.gptclient.room.entity.ChatRoomId
import net.matsudamper.gptclient.viewmodel.AddRequestUseCase

class JvmWorkManagerScheduler : AddRequestUseCase.WorkManagerScheduler {
    override fun scheduleWork(
        chatRoomId: ChatRoomId,
        message: String,
        uris: List<String>,
    ): String {
        return "jvm-dummy-work-id"
    }

    override fun isWorkRunning(workId: String): Boolean {
        return false
    }
}
