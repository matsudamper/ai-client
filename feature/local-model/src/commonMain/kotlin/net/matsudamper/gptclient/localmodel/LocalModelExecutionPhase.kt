package net.matsudamper.gptclient.localmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * ローカルモデルの推論が今どの段階にいるか。
 * 生成が始まるまでの待ち時間が長いため、UI で段階を出し分ける。
 */
enum class LocalModelExecutionPhase {
    WaitingForOtherModel,
    LoadingModel,
    Generating,
}

/**
 * 実行中のローカルモデルの段階を保持する。
 * 推論は端末内で直列に走るため、プロセス内で一つの状態を共有する。
 */
object LocalModelExecutionPhaseStore {
    private val mutablePhases = MutableStateFlow<Map<LocalModelId, LocalModelExecutionPhase>>(mapOf())
    val phases: StateFlow<Map<LocalModelId, LocalModelExecutionPhase>> = mutablePhases

    fun update(modelId: LocalModelId, phase: LocalModelExecutionPhase) {
        mutablePhases.update { it + (modelId to phase) }
    }

    fun clear(modelId: LocalModelId) {
        mutablePhases.update { it - modelId }
    }
}
