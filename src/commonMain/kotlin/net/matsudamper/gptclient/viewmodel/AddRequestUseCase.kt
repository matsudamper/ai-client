package net.matsudamper.gptclient.viewmodel

import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.matsudamper.gptclient.client.AiClient
import net.matsudamper.gptclient.room.AppDatabase
import net.matsudamper.gptclient.room.entity.Chat
import net.matsudamper.gptclient.room.entity.ChatRoomId

class AddRequestUseCase(
    private val appDatabase: AppDatabase,
    private val workManagerScheduler: WorkManagerScheduler,
) {
    private val startMutex = Mutex()
    private val cancelReservation = WorkStartCancelReservation()

    suspend fun addRequest(
        chatRoomId: ChatRoomId,
        message: String,
        uris: List<String>,
    ): Result {
        if (message.isEmpty() && uris.isEmpty()) return Result.IsLastUserChat

        return withContext(Dispatchers.IO) {
            whileStartingRequest(chatRoomId = chatRoomId) start@{
                if (isWorkRunning(chatRoomId = chatRoomId)) {
                    return@start Result.WorkInProgress
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

                startWork(chatRoomId = chatRoomId)

                Result.Success
            }
        }
    }

    suspend fun retryRequest(chatRoomId: ChatRoomId): Result {
        return withContext(Dispatchers.IO) {
            whileStartingRequest(chatRoomId = chatRoomId) start@{
                if (isWorkRunning(chatRoomId = chatRoomId)) {
                    return@start Result.WorkInProgress
                }

                val chats = appDatabase.chatDao().get(chatRoomId = chatRoomId.value).first()

                if (chats.none { it.role == Chat.Role.User }) {
                    return@start Result.IsLastUserChat
                }

                startWork(chatRoomId = chatRoomId)

                Result.Success
            }
        }
    }

    private suspend fun isWorkRunning(chatRoomId: ChatRoomId): Boolean {
        val workerId = appDatabase.chatRoomDao().get(chatRoomId = chatRoomId.value).first().workerId
            ?: return false
        return workManagerScheduler.hasWork(workerId)
    }

    /**
     * 実行中の確認から workerId の保存までを直列化する。並行した開始処理が
     * 互いの workerId を後から上書きすると、実行中の Work を見失う。
     *
     * UI は開始操作の直後からキャンセルを表示するため、開始処理の全体をキャンセル予約の対象にする。
     */
    private suspend fun <T> whileStartingRequest(chatRoomId: ChatRoomId, block: suspend () -> T): T {
        return startMutex.withLock {
            cancelReservation.beginStart(chatRoomId = chatRoomId)
            try {
                block()
            } finally {
                withContext(NonCancellable) {
                    if (cancelReservation.endStartAndTakeCancel(chatRoomId = chatRoomId)) {
                        cancelScheduledWork(chatRoomId = chatRoomId)
                    }
                }
            }
        }
    }

    /**
     * Work は登録直後に実行され得る。Worker が書き込んだ結果を上書きしないよう、
     * 前回の実行結果は登録前に消し、登録後は workerId の列だけを更新する。
     *
     * 登録の完了待ちはコルーチンのキャンセルに反応しないため、workerId を保存するまでは中断させない。
     */
    private suspend fun startWork(chatRoomId: ChatRoomId) {
        withContext(NonCancellable) {
            val chatRoomDao = appDatabase.chatRoomDao()
            chatRoomDao.clearRequestState(chatRoomId = chatRoomId.value)

            val workId = workManagerScheduler.scheduleWork(chatRoomId = chatRoomId)
            chatRoomDao.updateWorkerId(chatRoomId = chatRoomId.value, workerId = workId)
            if (workManagerScheduler.hasWork(workId).not()) {
                clearWorkerStateIfMatches(chatRoomId = chatRoomId, workerId = workId)
            }
        }
    }

    suspend fun cancelRequest(chatRoomId: ChatRoomId) {
        withContext(Dispatchers.IO) {
            // キャンセル自体はエラーとして記録されないため、ここで明示的にメッセージを残し、
            // チャット側のリトライ導線（ChatErrorMessageRetryComposableInterface）を成立させる
            appDatabase.chatRoomDao().updateLatestErrorMessage(
                chatRoomId = chatRoomId.value,
                errorMessage = "キャンセルしました",
            )

            if (cancelReservation.reserveCancelIfStarting(chatRoomId = chatRoomId)) return@withContext

            cancelScheduledWork(chatRoomId = chatRoomId)
        }
    }

    /**
     * キャンセルの完了は待てないため、workerId の解放は Work が終了状態になるまで状態監視に任せる。
     */
    private suspend fun cancelScheduledWork(chatRoomId: ChatRoomId) {
        val workerId = appDatabase.chatRoomDao().get(chatRoomId = chatRoomId.value).first().workerId
            ?: return
        workManagerScheduler.cancelWork(workerId)
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

    /**
     * 実際の処理開始時刻を返す。強制終了後に別の Work として再開された場合も、
     * その回の実処理開始時刻のみを返すため、前回分と累積されない。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeProcessStartedAt(chatRoomId: ChatRoomId): Flow<Instant?> {
        return appDatabase.chatRoomDao().get(chatRoomId = chatRoomId.value)
            .map { it.workerId }
            .distinctUntilChanged()
            .flatMapLatest { workerId ->
                if (workerId == null) {
                    flowOf(null)
                } else {
                    workManagerScheduler.observeProcessStartedAt(workerId)
                }
            }
    }

    private suspend fun clearWorkerStateIfMatches(chatRoomId: ChatRoomId, workerId: String) {
        withContext(Dispatchers.IO) {
            appDatabase.chatRoomDao().clearWorkerId(chatRoomId = chatRoomId.value, workerId = workerId)
        }
    }

    /**
     * Work の登録中は workerId がまだ無くキャンセルできないため、要求を預かって登録完了後に適用する。
     */
    private class WorkStartCancelReservation {
        private val mutex = Mutex()
        private val startingRooms = mutableSetOf<ChatRoomId>()
        private val reservedRooms = mutableSetOf<ChatRoomId>()

        suspend fun beginStart(chatRoomId: ChatRoomId) {
            mutex.withLock {
                startingRooms.add(chatRoomId)
                reservedRooms.remove(chatRoomId)
            }
        }

        suspend fun endStartAndTakeCancel(chatRoomId: ChatRoomId): Boolean {
            return mutex.withLock {
                startingRooms.remove(chatRoomId)
                reservedRooms.remove(chatRoomId)
            }
        }

        suspend fun reserveCancelIfStarting(chatRoomId: ChatRoomId): Boolean {
            return mutex.withLock {
                if (startingRooms.contains(chatRoomId).not()) return@withLock false
                reservedRooms.add(chatRoomId)
                true
            }
        }
    }

    interface WorkManagerScheduler {
        suspend fun scheduleWork(
            chatRoomId: ChatRoomId,
        ): String

        fun cancelWork(workId: String)

        fun hasWork(workId: String): Boolean

        fun observeWorkInProgress(workId: String): Flow<Boolean>

        fun observeProcessStartedAt(workId: String): Flow<Instant?>
    }

    sealed interface Result {
        data object Success : Result
        data class GptResultError(val gptError: AiClient.GptResult.Error) : Result
        data object IsLastUserChat : Result
        data object ModelNotFoundError : Result
        data object WorkInProgress : Result
    }
}
