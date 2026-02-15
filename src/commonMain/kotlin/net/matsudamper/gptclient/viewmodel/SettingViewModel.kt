package net.matsudamper.gptclient.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.gptclient.PlatformRequest
import net.matsudamper.gptclient.datastore.SettingDataStore
import net.matsudamper.gptclient.datastore.ThemeMode
import net.matsudamper.gptclient.ui.SettingsScreenUiState

class SettingViewModel(
    private val settingDataStore: SettingDataStore,
    private val platformRequest: PlatformRequest,
) : ViewModel() {
    private val _uiStateFlow = MutableStateFlow<SettingsScreenUiState>(
        SettingsScreenUiState.Loading,
    )

    private val loadedListener = object : SettingsScreenUiState.Loaded.Listener {
        override fun updateSecretKey(text: String) {
            saveSecretKey(text)
        }

        override fun updateGeminiSecretKey(text: String) {
            saveGeminiSecretKey(text)
        }

        override fun onClickOpenAiUsage() {
            platformRequest.openLink(
                url = "https://platform.openai.com/settings/organization/usage",
            )
        }

        override fun onClickGeminiUsage() {
            platformRequest.openLink(
                url = "https://aistudio.google.com/usagecontinue",
            )
        }

        override fun onClickThemeMode(themeMode: ThemeMode) {
            viewModelScope.launch {
                settingDataStore.setThemeMode(themeMode)
            }
        }
    }

    val uiStateFlow: StateFlow<SettingsScreenUiState> = _uiStateFlow.also { uiState ->
        viewModelScope.launch {
            val secretKey = settingDataStore.getSecretKey()
            val geminiSecretKey = settingDataStore.getGeminiSecretKey()
            settingDataStore.getThemeModeFlow().collect { themeMode ->
                uiState.update {
                    val current = it as? SettingsScreenUiState.Loaded
                    SettingsScreenUiState.Loaded(
                        initialSecretKey = current?.initialSecretKey ?: secretKey,
                        initialGeminiSecretKey = current?.initialGeminiSecretKey ?: geminiSecretKey,
                        themeMode = themeMode,
                        listener = loadedListener,
                    )
                }
            }
        }
    }

    private fun saveSecretKey(text: String) {
        viewModelScope.launch {
            settingDataStore.setSecretKey(text)
        }
    }

    private fun saveGeminiSecretKey(text: String) {
        viewModelScope.launch {
            settingDataStore.setGeminiSecretKey(text)
        }
    }
}
