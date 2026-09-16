package net.matsudamper.gptclient.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import net.matsudamper.gptclient.client.AiClient
import net.matsudamper.gptclient.room.AppDatabase
import net.matsudamper.gptclient.room.entity.Chat
import net.matsudamper.gptclient.room.entity.ChatRoom
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

        return withContext(Dispatchers.IO) {
            val room = appDatabase.chatRoomDao().get(chatRoomId = chatRoomId.value).first()
            val workerId = room.workerId
            if (workerId != null && workManagerScheduler.hasWork(workerId)) {
                return@withContext Result.WorkInProgress
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

            startWork(chatRoomId = chatRoomId, room = room)

            Result.Success
        }
    }

    suspend fun retryRequest(chatRoomId: ChatRoomId): Result {
        return withContext(Dispatchers.IO) {
            val room = appDatabase.chatRoomDao().get(chatRoomId = chatRoomId.value).first()
            val chats = appDatabase.chatDao().get(chatRoomId = chatRoomId.value).first()

            if (chats.none { it.role == Chat.Role.User }) {
                return@withContext Result.IsLastUserChat
            }

            startWork(chatRoomId = chatRoomId, room = room)

            return@withContext Result.Success
        }
    }

    /**
     * Work は登録直後に実行され得るため、Worker が書き込んだ結果を上書きしないよう
     * 前回の実行結果は登録前に消し、workerId は最新の Room に対して書き込む。
     */
    private suspend fun startWork(chatRoomId: ChatRoomId, room: ChatRoom) {
        appDatabase.chatRoomDao().update(room.copy(workerId = null, latestErrorMessage = null))
        val workId = workManagerScheduler.scheduleWork(chatRoomId = chatRoomId)
        appDatabase.chatRoomDao().update(id = chatRoomId) { it.copy(workerId = workId) }
    }

    suspend fun cancelRequest(chatRoomId: ChatRoomId) {
        withContext(Dispatchers.IO) {
            val workerId = appDatabase.chatRoomDao().get(chatRoomId = chatRoomId.value).first().workerId
                ?: return@withContext
            workManagerScheduler.cancelWork(workerId)
        }
    }

    /**
     * 通知からのキャンセルなどアプリ外で Work が終了した場合も検知するため、DB の workerId ではなく Work の状態を監視する。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeWorkInProgress(chatRoomId: ChatRoomId): Flow<Boolean> {
        return appDatabase.chatRoomDao().get(chatRoomId = chatRoomId.value)
            .map { it.workerId }
            .distinctUntilChanged()
            .flatMapLatest { workerId ->
                if (workerId == null) {
                    flowOf(false)
                } else {
                    workManagerScheduler.observeWorkInProgress(workerId)
                        .onEach { inProgress ->
                            if (!inProgress) {
                                clearWorkerStateIfMatches(chatRoomId = chatRoomId, workerId = workerId)
                            }
                        }
                }
            }
            .distinctUntilChanged()
    }

    private suspend fun clearWorkerStateIfMatches(chatRoomId: ChatRoomId, workerId: String) {
        withContext(Dispatchers.IO) {
            appDatabase.chatRoomDao().update(id = chatRoomId) { room ->
                if (room.workerId == workerId) room.copy(workerId = null) else room
            }
        }
    }

    interface WorkManagerScheduler {
        fun scheduleWork(
            chatRoomId: ChatRoomId,
        ): String

        fun cancelWork(workId: String)

        fun hasWork(workId: String): Boolean

        fun observeWorkInProgress(workId: String): Flow<Boolean>
    }

    sealed interface Result {
        data object Success : Result
        data class GptResultError(val gptError: AiClient.GptResult.Error) : Result
        data object IsLastUserChat : Result
        data object ModelNotFoundError : Result
        data object WorkInProgress : Result
    }
}
