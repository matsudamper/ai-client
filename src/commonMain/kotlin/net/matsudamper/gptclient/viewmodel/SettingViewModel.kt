package net.matsudamper.gptclient.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.gptclient.PlatformRequest
import net.matsudamper.gptclient.datastore.SettingDataStore
import net.matsudamper.gptclient.datastore.ThemeMode
import net.matsudamper.gptclient.localmodel.LocalModelDefinition
import net.matsudamper.gptclient.localmodel.LocalModelRepository
import net.matsudamper.gptclient.localmodel.LocalModelState
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
    val uiStateFlow: StateFlow<SettingsScreenUiState> = _uiStateFlow

    private val modelsFlow = MutableStateFlow<List<LocalModelDefinition>>(emptyList())
    private val modelStateMap = MutableStateFlow<Map<String, LocalModelState>>(emptyMap())
    private val pendingDeleteModelId = MutableStateFlow<String?>(null)

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

    init {
        viewModelScope.launch {
            modelsFlow.value = localModelRepository.getModels()
        }
        viewModelScope.launch {
            localModelRepository.observeStatuses().collect { statuses ->
                modelStateMap.value = statuses
            }
        }
        viewModelScope.launch {
            val secretKey = settingDataStore.getSecretKey()
            val geminiSecretKey = settingDataStore.getGeminiSecretKey()
            val geminiBillingKey = settingDataStore.getGeminiBillingKey()

            combine(
                settingDataStore.getThemeModeFlow(),
                settingDataStore.getActiveLocalModelKeysFlow(),
                modelsFlow,
                modelStateMap,
                pendingDeleteModelId,
            ) { themeMode, activeKeys, models, statuses, deleteModelId ->
                ViewState(
                    themeMode = themeMode,
                    activeKeys = activeKeys,
                    models = models,
                    statuses = statuses,
                    deleteModelId = deleteModelId,
                )
            }.collect { state ->
                val current = _uiStateFlow.value as? SettingsScreenUiState.Loaded
                _uiStateFlow.value =
                    SettingsScreenUiState.Loaded(
                        initialSecretKey = current?.initialSecretKey ?: secretKey,
                        initialGeminiSecretKey = current?.initialGeminiSecretKey ?: geminiSecretKey,
                        initialGeminiBillingKey = current?.initialGeminiBillingKey ?: geminiBillingKey,
                        themeOption = state.themeMode.toUiState(),
                        localModels = state.models.map { model ->
                            model.toUiItem(
                                modelState = state.statuses[model.modelId]
                                    ?: LocalModelState(LocalModelStatus.NOT_DOWNLOADED),
                                isActive = model.modelId in state.activeKeys,
                            )
                        },
                        deleteDialog = state.deleteModelId
                            ?.let { deleteModelId ->
                                val model = state.models.firstOrNull { it.modelId == deleteModelId } ?: return@let null
                                SettingsScreenUiState.DeleteDialog(
                                    modelName = model.displayName,
                                    listener = createDeleteDialogListener(deleteModelId),
                                )
                            },
                        listener = loadedListener,
                    )
            }
        }
    }

    private fun createModelListener(modelId: String) =
        object : SettingsScreenUiState.LocalModelItem.Listener {
            override fun onClickDownload() {
                viewModelScope.launch {
                    localModelRepository.enqueueDownload(modelId)
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

            override fun onClickDelete() {
                pendingDeleteModelId.value = modelId
            }
        }

    private fun createDeleteDialogListener(modelId: String) =
        object : SettingsScreenUiState.DeleteDialog.Listener {
            override fun onConfirm() {
                viewModelScope.launch {
                    localModelRepository.delete(modelId)
                    settingDataStore.removeActiveLocalModelKey(modelId)
                    pendingDeleteModelId.value = null
                }
            }

            override fun onDismiss() {
                pendingDeleteModelId.value = null
            }
        }

    private fun LocalModelDefinition.toUiItem(
        modelState: LocalModelState,
        isActive: Boolean,
    ): SettingsScreenUiState.LocalModelItem {
        val status =
            when (modelState.status) {
                LocalModelStatus.NOT_DOWNLOADED -> SettingsScreenUiState.LocalModelItem.ModelStatus.NOT_DOWNLOADED
                LocalModelStatus.DOWNLOADING -> SettingsScreenUiState.LocalModelItem.ModelStatus.DOWNLOADING
                LocalModelStatus.DOWNLOADED -> SettingsScreenUiState.LocalModelItem.ModelStatus.DOWNLOADED
            }
        return SettingsScreenUiState.LocalModelItem(
            modelId = modelId,
            displayName = displayName,
            description = description,
            status = status,
            downloadProgress = modelState.progress,
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

    private data class ViewState(
        val themeMode: ThemeMode,
        val activeKeys: Set<String>,
        val models: List<LocalModelDefinition>,
        val statuses: Map<String, LocalModelState>,
        val deleteModelId: String?,
    )
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
