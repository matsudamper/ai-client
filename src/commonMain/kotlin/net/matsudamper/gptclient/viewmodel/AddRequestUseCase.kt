package net.matsudamper.gptclient.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import net.matsudamper.gptclient.client.AiClient
import net.matsudamper.gptclient.room.AppDatabase
import net.matsudamper.gptclient.room.entity.Chat
import net.matsudamper.gptclient.room.entity.ChatRoomId

class AddRequestUseCase(
    private val appDatabase: AppDatabase,
    private val workManagerScheduler: WorkManagerScheduler,
) {
    suspend fun addRequest(
        chatRoomId: ChatRoomId,
        message: String,
        uris: List<String>,
    ): Result {
        if (message.isEmpty() && uris.isEmpty()) return Result.IsLastUserChat

        withContext(Dispatchers.IO) {
            var room = appDatabase.chatRoomDao().get(chatRoomId = chatRoomId.value).first()
            val workerId = room.workerId
            if (workerId != null) {
                if (workManagerScheduler.hasWork(workerId)) {
                    return@withContext Result.WorkInProgress
                }
                room = room.copy(workerId = null)
                appDatabase.chatRoomDao().update(room)
            }

            val chatDao = appDatabase.chatDao()
            val lastItem = chatDao.getChatRoomLastIndexItem(
                chatRoomId = chatRoomId.value,
            )
            val newChatIndex = lastItem?.index?.plus(1) ?: 0

            chatDao.insertAll(
                uris.map {
                    Chat(
                        chatRoomId = chatRoomId,
                        index = newChatIndex,
                        textMessage = null,
                        imageUri = it,
                        role = Chat.Role.User,
                    )
                },
            )
            if (message.isNotEmpty()) {
                chatDao.insertAll(
                    Chat(
                        chatRoomId = chatRoomId,
                        index = newChatIndex,
                        textMessage = message,
                        imageUri = null,
                        role = Chat.Role.User,
                    ),
                )
            }

            val workId = workManagerScheduler.scheduleWork(
                chatRoomId = chatRoomId,
            )
            appDatabase.chatRoomDao().update(
                room.copy(
                    workerId = workId,
                    latestErrorMessage = null,
                ),
            )
        }

        return Result.Success
    }

    suspend fun retryRequest(chatRoomId: ChatRoomId): Result {
        return withContext(Dispatchers.IO) {
            val room = appDatabase.chatRoomDao().get(chatRoomId = chatRoomId.value).first()
            appDatabase.chatRoomDao().update(room.copy(workerId = null))

            val chats = appDatabase.chatDao().get(chatRoomId = chatRoomId.value).first()

            if (chats.none { it.role == Chat.Role.User }) {
                return@withContext Result.IsLastUserChat
            }

            val workId = workManagerScheduler.scheduleWork(
                chatRoomId = chatRoomId,
            )
            appDatabase.chatRoomDao().update(
                room.copy(
                    workerId = workId,
                    latestErrorMessage = null,
                ),
            )

            return@withContext Result.Success
        }
    }

    suspend fun cancelRequest(chatRoomId: ChatRoomId) {
        withContext(Dispatchers.IO) {
            val room = appDatabase.chatRoomDao().get(chatRoomId = chatRoomId.value).first()
            val workerId = room.workerId ?: return@withContext
            if (!workManagerScheduler.hasWork(workerId)) {
                appDatabase.chatRoomDao().update(room.copy(workerId = null, latestErrorMessage = null))
                return@withContext
            }
            workManagerScheduler.cancelWork(workerId)
            if (!workManagerScheduler.hasWork(workerId)) {
                val latestRoom = appDatabase.chatRoomDao().get(chatRoomId = chatRoomId.value).first()
                if (latestRoom.workerId == workerId) {
                    appDatabase.chatRoomDao().update(
                        latestRoom.copy(workerId = null, latestErrorMessage = null),
                    )
                }
            }
        }
    }

    suspend fun isWorkInProgress(chatRoomId: ChatRoomId): Boolean {
        val room = appDatabase.chatRoomDao().get(chatRoomId = chatRoomId.value).first()
        val workerId = room.workerId ?: return false
        if (workManagerScheduler.hasWork(workerId)) return true

        appDatabase.chatRoomDao().update(room.copy(workerId = null, latestErrorMessage = null))
        return false
    }

    interface WorkManagerScheduler {
        fun scheduleWork(
            chatRoomId: ChatRoomId,
        ): String

        fun cancelWork(workId: String)

        fun hasWork(workId: String): Boolean
    }

    sealed interface Result {
        data object Success : Result
        data class GptResultError(val gptError: AiClient.GptResult.Error) : Result
        data object IsLastUserChat : Result
        data object ModelNotFoundError : Result
        data object WorkInProgress : Result
    }
}
