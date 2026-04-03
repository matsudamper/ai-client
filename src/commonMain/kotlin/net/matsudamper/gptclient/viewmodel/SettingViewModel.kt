package net.matsudamper.gptclient.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.gptclient.PlatformRequest
import net.matsudamper.gptclient.datastore.SettingDataStore
import net.matsudamper.gptclient.datastore.ThemeMode
import net.matsudamper.gptclient.entity.ChatGptModel
import net.matsudamper.gptclient.localmodel.DownloadProgress
import net.matsudamper.gptclient.localmodel.LocalModelRepository
import net.matsudamper.gptclient.localmodel.LocalModelStatus
import net.matsudamper.gptclient.ui.SettingsScreenUiState
import net.matsudamper.gptclient.util.EventSender

class SettingViewModel(
    private val settingDataStore: SettingDataStore,
    private val localModelRepository: LocalModelRepository,
) : ViewModel() {
    private val eventSender = EventSender<Event>()
    val eventHandler = eventSender.asHandler()

    interface Event {
        fun providePlatformRequest(): PlatformRequest
    }

    private val _uiStateFlow = MutableStateFlow<SettingsScreenUiState>(
        SettingsScreenUiState.Loading,
    )

    private val localModelStatusFlow = MutableStateFlow<LocalModelStatus?>(null)
    private val localModelKey = ChatGptModel.Local().modelKey

    private val localModelListener = object : SettingsScreenUiState.LocalModelUiState.Available.Listener {
        override fun onClickDownload() {
            viewModelScope.launch {
                localModelStatusFlow.value = LocalModelStatus.DOWNLOADING
                try {
                    localModelRepository.download().collectLatest { progress ->
                        when (progress) {
                            is DownloadProgress.Started,
                            is DownloadProgress.InProgress,
                            -> localModelStatusFlow.value = LocalModelStatus.DOWNLOADING

                            is DownloadProgress.Completed -> localModelStatusFlow.value = LocalModelStatus.AVAILABLE
                            is DownloadProgress.Failed -> localModelStatusFlow.value = LocalModelStatus.DOWNLOADABLE
                        }
                    }
                } catch (e: Exception) {
                    localModelStatusFlow.value = LocalModelStatus.DOWNLOADABLE
                }
            }
        }

        override fun onToggleActive(active: Boolean) {
            viewModelScope.launch {
                if (active) {
                    settingDataStore.addActiveLocalModelKey(localModelKey)
                } else {
                    settingDataStore.removeActiveLocalModelKey(localModelKey)
                }
            }
        }
    }

    private val loadedListener = object : SettingsScreenUiState.Loaded.Listener {
        override fun updateSecretKey(text: String) {
            saveSecretKey(text)
        }

        override fun updateGeminiSecretKey(text: String) {
            saveGeminiSecretKey(text)
        }

        override fun updateGeminiBillingKey(text: String) {
            saveGeminiBillingKey(text)
        }

        override fun onClickOpenAiUsage() {
            launchWithPlatformRequest {
                openLink(
                    url = "https://platform.openai.com/settings/organization/usage",
                )
            }
        }

        override fun onClickGeminiUsage() {
            launchWithPlatformRequest {
                openLink(
                    url = "https://aistudio.google.com/usagecontinue",
                )
            }
        }

        override fun onClickThemeOption(themeOption: SettingsScreenUiState.ThemeOption) {
            viewModelScope.launch {
                settingDataStore.setThemeMode(themeOption.toData())
            }
        }
    }

    val uiStateFlow: StateFlow<SettingsScreenUiState> = _uiStateFlow.also { uiState ->
        viewModelScope.launch {
            localModelStatusFlow.value = localModelRepository.checkStatus()
        }
        viewModelScope.launch {
            val secretKey = settingDataStore.getSecretKey()
            val geminiSecretKey = settingDataStore.getGeminiSecretKey()
            val geminiBillingKey = settingDataStore.getGeminiBillingKey()
            combine(
                settingDataStore.getThemeModeFlow(),
                localModelStatusFlow,
                settingDataStore.getActiveLocalModelKeysFlow(),
            ) { themeMode, localStatus, activeKeys ->
                Triple(themeMode, localStatus, activeKeys)
            }.collect { (themeMode, localStatus, activeKeys) ->
                uiState.update {
                    val current = it as? SettingsScreenUiState.Loaded
                    SettingsScreenUiState.Loaded(
                        initialSecretKey = current?.initialSecretKey ?: secretKey,
                        initialGeminiSecretKey = current?.initialGeminiSecretKey ?: geminiSecretKey,
                        initialGeminiBillingKey = current?.initialGeminiBillingKey ?: geminiBillingKey,
                        themeOption = themeMode.toUiState(),
                        localModel = localStatus.toLocalModelUiState(activeKeys),
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

    private fun saveGeminiBillingKey(text: String) {
        viewModelScope.launch {
            settingDataStore.setGeminiBillingKey(text)
        }
    }

    private fun launchWithPlatformRequest(
        block: suspend PlatformRequest.() -> Unit,
    ) {
        viewModelScope.launch {
            eventSender.send { event ->
                event.providePlatformRequest().block()
            }
        }
    }

    private fun LocalModelStatus?.toLocalModelUiState(
        activeKeys: Set<String>,
    ): SettingsScreenUiState.LocalModelUiState {
        return when (this) {
            null, LocalModelStatus.UNAVAILABLE -> SettingsScreenUiState.LocalModelUiState.Unavailable
            LocalModelStatus.DOWNLOADABLE -> SettingsScreenUiState.LocalModelUiState.Available(
                status = SettingsScreenUiState.LocalModelUiState.Available.Status.DOWNLOADABLE,
                isActive = localModelKey in activeKeys,
                listener = localModelListener,
            )

            LocalModelStatus.DOWNLOADING -> SettingsScreenUiState.LocalModelUiState.Available(
                status = SettingsScreenUiState.LocalModelUiState.Available.Status.DOWNLOADING,
                isActive = localModelKey in activeKeys,
                listener = localModelListener,
            )

            LocalModelStatus.AVAILABLE -> SettingsScreenUiState.LocalModelUiState.Available(
                status = SettingsScreenUiState.LocalModelUiState.Available.Status.DOWNLOADED,
                isActive = localModelKey in activeKeys,
                listener = localModelListener,
            )
        }
    }
}

private fun ThemeMode.toUiState(): SettingsScreenUiState.ThemeOption = when (this) {
    ThemeMode.SYSTEM -> SettingsScreenUiState.ThemeOption.SYSTEM
    ThemeMode.LIGHT -> SettingsScreenUiState.ThemeOption.LIGHT
    ThemeMode.DARK -> SettingsScreenUiState.ThemeOption.DARK
}

private fun SettingsScreenUiState.ThemeOption.toData(): ThemeMode = when (this) {
    SettingsScreenUiState.ThemeOption.SYSTEM -> ThemeMode.SYSTEM
    SettingsScreenUiState.ThemeOption.LIGHT -> ThemeMode.LIGHT
    SettingsScreenUiState.ThemeOption.DARK -> ThemeMode.DARK
}
