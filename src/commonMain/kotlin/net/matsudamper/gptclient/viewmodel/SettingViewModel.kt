package net.matsudamper.gptclient.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import net.matsudamper.gptclient.PlatformRequest
import net.matsudamper.gptclient.datastore.SettingDataStore
import net.matsudamper.gptclient.datastore.ThemeMode
import net.matsudamper.gptclient.localmodel.LocalModelDefinition
import net.matsudamper.gptclient.localmodel.LocalModelId
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

    interface Event { fun providePlatformRequest(): PlatformRequest }
    interface LifecycleListener { fun onStart() }

    private val _uiStateFlow = MutableStateFlow<SettingsScreenUiState>(SettingsScreenUiState.Loading)
    val uiStateFlow: StateFlow<SettingsScreenUiState> = _uiStateFlow
    private val modelsFlow = MutableStateFlow<List<LocalModelDefinition>>(emptyList())
    private val modelStateMap = MutableStateFlow<Map<LocalModelId, LocalModelState>>(emptyMap())
    private val pendingDeleteModelId = MutableStateFlow<LocalModelId?>(null)
    private val secretKeyState = MutableStateFlow("")
    private val geminiSecretKeyState = MutableStateFlow("")
    private val geminiBillingKeyState = MutableStateFlow("")

    private val loadedListener = object : SettingsScreenUiState.Loaded.Listener, LifecycleListener {
        override fun onStart() { viewModelScope.launch { modelsFlow.value = localModelRepository.getResolvedModels() } }
        override fun updateSecretKey(text: String) { secretKeyState.value = text; saveSecretKey(text) }
        override fun updateGeminiSecretKey(text: String) { geminiSecretKeyState.value = text; saveGeminiSecretKey(text) }
        override fun updateGeminiBillingKey(text: String) { geminiBillingKeyState.value = text; saveGeminiBillingKey(text) }
        override fun onClickOpenAiUsage() { launchWithPlatformRequest { openLink(url = "https://platform.openai.com/settings/organization/usage") } }
        override fun onClickGeminiUsage() { launchWithPlatformRequest { openLink(url = "https://aistudio.google.com/usagecontinue") } }
        override fun onClickLatestRelease() { launchWithPlatformRequest { openLink(url = "https://github.com/matsudamper/ai-client/releases") } }
        override fun onClickThemeOption(themeOption: SettingsScreenUiState.ThemeOption) { viewModelScope.launch { settingDataStore.setThemeMode(themeOption.toData()) } }
    }

    init {
        viewModelScope.launch { modelsFlow.value = localModelRepository.getResolvedModels() }
        viewModelScope.launch { localModelRepository.observeStatuses().collect { modelStateMap.value = it } }
        viewModelScope.launch {
            secretKeyState.value = settingDataStore.getSecretKey()
            geminiSecretKeyState.value = settingDataStore.getGeminiSecretKey()
            geminiBillingKeyState.value = settingDataStore.getGeminiBillingKey()
        }
        viewModelScope.launch {
            combine(
                combine(secretKeyState, geminiSecretKeyState, geminiBillingKeyState, settingDataStore.getThemeModeFlow(), settingDataStore.getActiveLocalModelKeysFlow()) { secretKey, geminiSecretKey, geminiBillingKey, themeMode, activeKeys -> KeyViewState(secretKey, geminiSecretKey, geminiBillingKey, themeMode, activeKeys) },
                combine(modelsFlow, modelStateMap, pendingDeleteModelId) { models, statuses, deleteModelId -> ModelViewState(models, statuses, deleteModelId) },
            ) { keyState, modelState -> ViewState(keyState.secretKey, keyState.geminiSecretKey, keyState.geminiBillingKey, keyState.themeMode, keyState.activeKeys, modelState.models, modelState.statuses, modelState.deleteModelId) }
                .collect { state ->
                    _uiStateFlow.value = SettingsScreenUiState.Loaded(
                        initialSecretKey = state.secretKey,
                        initialGeminiSecretKey = state.geminiSecretKey,
                        initialGeminiBillingKey = state.geminiBillingKey,
                        themeOption = state.themeMode.toUiState(),
                        localModelSections = createLocalModelSections(state.models, state.statuses, state.activeKeys),
                        deleteDialog = state.deleteModelId?.let { deleteModelId ->
                            val model = state.models.firstOrNull { it.modelId == deleteModelId } ?: return@let null
                            SettingsScreenUiState.DeleteDialog(model.displayName, createDeleteDialogListener(deleteModelId))
                        },
                        listener = loadedListener,
                    )
                }
        }
    }

    private fun createLocalModelSections(models: List<LocalModelDefinition>, statuses: Map<LocalModelId, LocalModelState>, activeKeys: Set<LocalModelId>): List<SettingsScreenUiState.LocalModelSection> =
        models.groupBy { it.section }.map { (section, sectionModels) ->
            val allCandidatesByDisplayName = sectionModels.groupBy { it.displayName }
            val visibleModelItems = sectionModels
                .filter { model -> !section.hideUnavailableModels || model.modelId in activeKeys || modelState(model, statuses).status != LocalModelStatus.UNAVAILABLE }
                .groupBy { it.displayName }
                .map { (displayName, candidates) ->
                    val allCandidates = allCandidatesByDisplayName.getValue(displayName)
                    val model = selectVisibleModel(candidates, statuses, activeKeys)
                    model.toUiItem(modelState(model, statuses), allCandidates.any { it.modelId in activeKeys }, allCandidates.mapTo(linkedSetOf()) { it.modelId })
                }
            SettingsScreenUiState.LocalModelSection(section.displayName, visibleModelItems, if (visibleModelItems.isEmpty()) section.unavailableMessage else null)
        }

    private fun selectVisibleModel(candidates: List<LocalModelDefinition>, statuses: Map<LocalModelId, LocalModelState>, activeKeys: Set<LocalModelId>): LocalModelDefinition =
        candidates.firstOrNull { it.modelId in activeKeys } ?: candidates.firstOrNull { modelState(it, statuses).status == LocalModelStatus.DOWNLOADED } ?: candidates.firstOrNull { modelState(it, statuses).status == LocalModelStatus.DOWNLOADING } ?: candidates.first()

    private fun modelState(model: LocalModelDefinition, statuses: Map<LocalModelId, LocalModelState>): LocalModelState = statuses[model.modelId] ?: LocalModelState(if (model.section.hideUnavailableModels) LocalModelStatus.UNAVAILABLE else LocalModelStatus.NOT_DOWNLOADED)

    private fun createModelListener(modelId: LocalModelId, groupedModelIds: Set<LocalModelId>) = object : SettingsScreenUiState.LocalModelItem.Listener {
        override fun onClickDownload() { viewModelScope.launch { localModelRepository.enqueueDownload(modelId) } }
        override fun onToggleActive(active: Boolean) { viewModelScope.launch { if (active) settingDataStore.addActiveLocalModelKey(modelId) else groupedModelIds.forEach { settingDataStore.removeActiveLocalModelKey(it) } } }
        override fun onClickDelete() { pendingDeleteModelId.value = modelId }
    }

    private fun createDeleteDialogListener(modelId: LocalModelId) = object : SettingsScreenUiState.DeleteDialog.Listener {
        override fun onConfirm() { viewModelScope.launch { localModelRepository.delete(modelId); settingDataStore.removeActiveLocalModelKey(modelId); pendingDeleteModelId.value = null } }
        override fun onDismiss() { pendingDeleteModelId.value = null }
    }

    private fun LocalModelDefinition.toUiItem(modelState: LocalModelState, isActive: Boolean, groupedModelIds: Set<LocalModelId>): SettingsScreenUiState.LocalModelItem {
        val status = when (modelState.status) {
            LocalModelStatus.UNAVAILABLE -> SettingsScreenUiState.LocalModelItem.ModelStatus.UNAVAILABLE
            LocalModelStatus.NOT_DOWNLOADED -> SettingsScreenUiState.LocalModelItem.ModelStatus.NOT_DOWNLOADED
            LocalModelStatus.DOWNLOADING -> SettingsScreenUiState.LocalModelItem.ModelStatus.DOWNLOADING
            LocalModelStatus.DOWNLOADED -> SettingsScreenUiState.LocalModelItem.ModelStatus.DOWNLOADED
        }
        return SettingsScreenUiState.LocalModelItem(displayName, description, status, modelState.progress, canDelete, isActive, createModelListener(modelId, groupedModelIds))
    }

    private fun saveSecretKey(text: String) { viewModelScope.launch { settingDataStore.setSecretKey(text) } }
    private fun saveGeminiSecretKey(text: String) { viewModelScope.launch { settingDataStore.setGeminiSecretKey(text) } }
    private fun saveGeminiBillingKey(text: String) { viewModelScope.launch { settingDataStore.setGeminiBillingKey(text) } }
    private fun launchWithPlatformRequest(block: suspend PlatformRequest.() -> Unit) { viewModelScope.launch { eventSender.send { it.providePlatformRequest().block() } } }

    private data class KeyViewState(val secretKey: String, val geminiSecretKey: String, val geminiBillingKey: String, val themeMode: ThemeMode, val activeKeys: Set<LocalModelId>)
    private data class ModelViewState(val models: List<LocalModelDefinition>, val statuses: Map<LocalModelId, LocalModelState>, val deleteModelId: LocalModelId?)
    private data class ViewState(val secretKey: String, val geminiSecretKey: String, val geminiBillingKey: String, val themeMode: ThemeMode, val activeKeys: Set<LocalModelId>, val models: List<LocalModelDefinition>, val statuses: Map<LocalModelId, LocalModelState>, val deleteModelId: LocalModelId?)
}

private fun ThemeMode.toUiState(): SettingsScreenUiState.ThemeOption = when (this) { ThemeMode.SYSTEM -> SettingsScreenUiState.ThemeOption.SYSTEM; ThemeMode.LIGHT -> SettingsScreenUiState.ThemeOption.LIGHT; ThemeMode.DARK -> SettingsScreenUiState.ThemeOption.DARK }
private fun SettingsScreenUiState.ThemeOption.toData(): ThemeMode = when (this) { SettingsScreenUiState.ThemeOption.SYSTEM -> ThemeMode.SYSTEM; SettingsScreenUiState.ThemeOption.LIGHT -> ThemeMode.LIGHT; SettingsScreenUiState.ThemeOption.DARK -> ThemeMode.DARK }
