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
import net.matsudamper.gptclient.localmodel.DownloadProgress
import net.matsudamper.gptclient.localmodel.LocalModelDefinition
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

    private val modelStatusMap = MutableStateFlow<Map<String, LocalModelStatus>>(emptyMap())

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

    private fun createModelListener(modelId: String) =
        object : SettingsScreenUiState.LocalModelItem.Listener {
            override fun onClickDownload() {
                viewModelScope.launch {
                    modelStatusMap.update { it + (modelId to LocalModelStatus.DOWNLOADING) }
                    try {
                        localModelRepository.download(modelId).collectLatest { progress ->
                            when (progress) {
                                is DownloadProgress.Started,
                                is DownloadProgress.InProgress,
                                -> modelStatusMap.update { it + (modelId to LocalModelStatus.DOWNLOADING) }

                                is DownloadProgress.Completed ->
                                    modelStatusMap.update { it + (modelId to LocalModelStatus.AVAILABLE) }

                                is DownloadProgress.Failed ->
                                    modelStatusMap.update { it + (modelId to LocalModelStatus.DOWNLOADABLE) }
                            }
                        }
                    } catch (e: Exception) {
                        modelStatusMap.update { it + (modelId to LocalModelStatus.DOWNLOADABLE) }
                    }
                }
            }

            override fun onToggleActive(active: Boolean) {
                viewModelScope.launch {
                    if (active) {
                        settingDataStore.addActiveLocalModelKey(modelId)
                    } else {
                        settingDataStore.removeActiveLocalModelKey(modelId)
                    }
                }
            }
        }

    val uiStateFlow: StateFlow<SettingsScreenUiState> = _uiStateFlow.also { uiState ->
        val models = localModelRepository.getModels()
        viewModelScope.launch {
            val initialStatuses = mutableMapOf<String, LocalModelStatus>()
            for (model in models) {
                initialStatuses[model.modelId] = localModelRepository.checkStatus(model.modelId)
            }
            modelStatusMap.value = initialStatuses
        }
        viewModelScope.launch {
            val secretKey = settingDataStore.getSecretKey()
            val geminiSecretKey = settingDataStore.getGeminiSecretKey()
            val geminiBillingKey = settingDataStore.getGeminiBillingKey()
            combine(
                settingDataStore.getThemeModeFlow(),
                modelStatusMap,
                settingDataStore.getActiveLocalModelKeysFlow(),
            ) { themeMode, statuses, activeKeys ->
                Triple(themeMode, statuses, activeKeys)
            }.collect { (themeMode, statuses, activeKeys) ->
                uiState.update {
                    val current = it as? SettingsScreenUiState.Loaded
                    SettingsScreenUiState.Loaded(
                        initialSecretKey = current?.initialSecretKey ?: secretKey,
                        initialGeminiSecretKey = current?.initialGeminiSecretKey ?: geminiSecretKey,
                        initialGeminiBillingKey = current?.initialGeminiBillingKey ?: geminiBillingKey,
                        themeOption = themeMode.toUiState(),
                        localModels = models.mapNotNull { model ->
                            val status = statuses[model.modelId] ?: return@mapNotNull null
                            if (status == LocalModelStatus.UNAVAILABLE) return@mapNotNull null
                            model.toUiItem(status, model.modelId in activeKeys)
                        },
                        listener = loadedListener,
                    )
                }
            }
        }
    }

    private fun LocalModelDefinition.toUiItem(
        status: LocalModelStatus,
        isActive: Boolean,
    ): SettingsScreenUiState.LocalModelItem {
        val uiStatus = when (status) {
            LocalModelStatus.AVAILABLE -> SettingsScreenUiState.LocalModelItem.ModelStatus.DOWNLOADED
            LocalModelStatus.DOWNLOADING -> SettingsScreenUiState.LocalModelItem.ModelStatus.DOWNLOADING
            LocalModelStatus.DOWNLOADABLE -> SettingsScreenUiState.LocalModelItem.ModelStatus.DOWNLOADABLE
            LocalModelStatus.UNAVAILABLE -> SettingsScreenUiState.LocalModelItem.ModelStatus.DOWNLOADABLE
        }
        return SettingsScreenUiState.LocalModelItem(
            modelId = modelId,
            displayName = displayName,
            description = description,
            status = uiStatus,
            isActive = isActive,
            listener = createModelListener(modelId),
        )
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
