package net.matsudamper.gptclient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.gptclient.ui.SettingsScreenUiState
import net.matsudamper.gptclient.datastore.SettingDataStore

class SettingViewModel(
    private val settingDataStore: SettingDataStore
) : ViewModel() {
    private val loadedListener = object : SettingsScreenUiState.Loaded.Listener {
        override fun updateSecretKey(text: String) {
            saveSecretKey(text)
        }
    }

    val uiStateFlow: StateFlow<SettingsScreenUiState> = MutableStateFlow<SettingsScreenUiState>(
        SettingsScreenUiState.Loading
    ).also { uiState ->
        viewModelScope.launch {
            uiState.update {
                SettingsScreenUiState.Loaded(
                    initialSecretKey = settingDataStore.getSecretKey(),
                    listener = loadedListener,
                )
            }
        }
    }

    private fun saveSecretKey(text: String) {
        viewModelScope.launch {
            settingDataStore.setSecretKey(text)
        }
    }
}
