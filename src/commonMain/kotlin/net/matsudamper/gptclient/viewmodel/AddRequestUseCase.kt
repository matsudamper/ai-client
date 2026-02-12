package net.matsudamper.gptclient.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import net.matsudamper.gptclient.PlatformRequest
import net.matsudamper.gptclient.client.AiClient
import net.matsudamper.gptclient.datastore.SettingDataStore
import net.matsudamper.gptclient.room.AppDatabase
import net.matsudamper.gptclient.room.entity.Chat
import net.matsudamper.gptclient.room.entity.ChatRoomId

class AddRequestUseCase(
    private val appDatabase: AppDatabase,
    private val platformRequest: PlatformRequest,
    private val gptClientProvider: (secretKey: String) -> AiClient,
    private val settingDataStore: SettingDataStore,
    private val workManagerScheduler: WorkManagerScheduler,
) {
    suspend fun addRequest(
        chatRoomId: ChatRoomId,
        message: String,
        uris: List<String>,
    ): Result {
        if (message.isEmpty() && uris.isEmpty()) return Result.IsLastUserChat

        withContext(Dispatchers.IO) {
            val room = appDatabase.chatRoomDao().get(chatRoomId = chatRoomId.value).first()
            val workerId = room.workerId
            if (workerId != null) {
                val isRunning = workManagerScheduler.isWorkRunning(workerId)
                if (isRunning) {
                    return@withContext Result.WorkInProgress
                }
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
                message = message,
                uris = uris,
            )
            appDatabase.chatRoomDao().update(room.copy(workerId = workId))
        }

        return Result.Success
    }

    suspend fun retryRequest(chatRoomId: ChatRoomId): Result {
        return withContext(Dispatchers.IO) {
            val room = appDatabase.chatRoomDao().get(chatRoomId = chatRoomId.value).first()
            appDatabase.chatRoomDao().update(room.copy(workerId = null))

            val chats = appDatabase.chatDao().get(chatRoomId = chatRoomId.value).first()

            val lastUserMessage = chats.lastOrNull { it.role == Chat.Role.User }
            if (lastUserMessage == null) {
                return@withContext Result.IsLastUserChat
            }

            val lastUserMessages = chats.filter {
                it.role == Chat.Role.User && it.index == lastUserMessage.index
            }
            val message = lastUserMessages.firstOrNull { it.textMessage != null }?.textMessage.orEmpty()
            val uris = lastUserMessages.mapNotNull { it.imageUri }

            val workId = workManagerScheduler.scheduleWork(
                chatRoomId = chatRoomId,
                message = message,
                uris = uris,
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

    suspend fun isWorkInProgress(chatRoomId: ChatRoomId): Boolean {
        val room = appDatabase.chatRoomDao().get(chatRoomId = chatRoomId.value).first()
        return room.workerId?.let { workManagerScheduler.isWorkRunning(it) } ?: false
    }

    interface WorkManagerScheduler {
        fun scheduleWork(
            chatRoomId: ChatRoomId,
            message: String,
            uris: List<String>,
        ): String

        fun isWorkRunning(workId: String): Boolean
    }

    sealed interface Result {
        data object Success : Result
        data class GptResultError(val gptError: AiClient.GptResult.Error) : Result
        data object IsLastUserChat : Result
        data object ModelNotFoundError : Result
        data object WorkInProgress : Result
    }
}
