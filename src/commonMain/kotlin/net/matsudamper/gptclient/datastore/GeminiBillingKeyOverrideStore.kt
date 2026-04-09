package net.matsudamper.gptclient.datastore

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Gemini モデルごとに BillingKey を使うかどうかのセッション中オーバーライドを保持する。
 * プロセス内シングルトンで、再起動するとリセットされる。
 */
object GeminiBillingKeyOverrideStore {
    private val _enabledSelectionKeys = MutableStateFlow<Set<String>>(emptySet())
    val enabledSelectionKeys: StateFlow<Set<String>> = _enabledSelectionKeys.asStateFlow()

    fun setEnabled(selectionKey: String, enabled: Boolean) {
        _enabledSelectionKeys.update { current ->
            if (enabled) current + selectionKey else current - selectionKey
        }
    }
}
