package net.matsudamper.gptclient.localmodel

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * ローカルモデルの準備と推論を直列化するキュー。
 * 端末のリソースを取り合って全ての処理が終わらなくなるのを防ぐため、同時にロードするモデルは常に一つに保つ。
 */
class LocalModelExecutionQueue(
    private val loadedModelStore: LoadedLocalModelStore,
) {
    private val stateLock = Mutex()
    private val waitingEntries = mutableListOf<WaitingEntry>()
    private var runningModelId: LocalModelId? = null
    private var loadedModelId: LocalModelId? = null
    private var continuousPriorityCount = 0

    suspend fun <T> withExclusiveModel(
        modelId: LocalModelId,
        block: suspend () -> T,
    ): T {
        acquireTurn(modelId)
        try {
            unloadOtherModels(modelId)
            return block()
        } finally {
            withContext(NonCancellable) { releaseTurn() }
        }
    }

    private suspend fun acquireTurn(modelId: LocalModelId) {
        val entry = stateLock.withLock {
            if (runningModelId == null) {
                runningModelId = modelId
                return
            }
            WaitingEntry(modelId = modelId, turn = CompletableDeferred())
                .also { waitingEntries.add(it) }
        }

        try {
            entry.turn.await()
        } catch (e: CancellationException) {
            withContext(NonCancellable) {
                val hasTurn = stateLock.withLock {
                    waitingEntries.remove(entry)
                    entry.turn.isCompleted
                }
                if (hasTurn) releaseTurn()
            }
            throw e
        }
    }

    /**
     * ロード済みモデルと同じモデルの待機を優先して、モデルのロードとアンロードの回数を減らす。
     * ただし優先が続くと別モデルの待機が開始されなくなるため、連続して優先できる回数を制限する。
     */
    private suspend fun releaseTurn() {
        stateLock.withLock {
            runningModelId = null

            val oldestEntry = waitingEntries.firstOrNull() ?: return
            val nextEntry = if (continuousPriorityCount < MAX_CONTINUOUS_PRIORITY_COUNT) {
                waitingEntries.firstOrNull { it.modelId == loadedModelId } ?: oldestEntry
            } else {
                oldestEntry
            }
            continuousPriorityCount = if (nextEntry === oldestEntry) 0 else continuousPriorityCount + 1
            waitingEntries.remove(nextEntry)
            runningModelId = nextEntry.modelId
            nextEntry.turn.complete(Unit)
        }
    }

    private suspend fun unloadOtherModels(modelId: LocalModelId) {
        val isLoaded = stateLock.withLock { loadedModelId == modelId }
        if (isLoaded) return

        loadedModelStore.unloadExcept(modelId)
        stateLock.withLock { loadedModelId = modelId }
    }

    private class WaitingEntry(
        val modelId: LocalModelId,
        val turn: CompletableDeferred<Unit>,
    )

    private companion object {
        private const val MAX_CONTINUOUS_PRIORITY_COUNT = 5
    }
}

/**
 * ロード済みのローカルモデルを保持する側へのアンロード指示。
 */
interface LoadedLocalModelStore {
    suspend fun unloadExcept(modelId: LocalModelId)
}
