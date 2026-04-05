package net.matsudamper.gptclient.viewmodel

import net.matsudamper.gptclient.entity.ChatGptModel
import net.matsudamper.gptclient.localmodel.LocalModelDefinition
import net.matsudamper.gptclient.localmodel.LocalModelId
import net.matsudamper.gptclient.localmodel.toChatGptModel
import net.matsudamper.gptclient.ui.component.ModelSelectorUiState

internal object ModelSelectorStateFactory {
    fun create(
        selectedModel: ChatGptModel,
        activeLocalModelKeys: Set<LocalModelId>,
        localModelDefs: List<LocalModelDefinition>,
        onSelectModel: (ChatGptModel) -> Unit,
    ): ModelSelectorUiState {
        val selectableModels = createSelectableModels(
            activeLocalModelKeys = activeLocalModelKeys,
            localModelDefs = localModelDefs,
        )

        return ModelSelectorUiState(
            selectedModelName = selectedModel.displayName,
            models = selectableModels.map { model ->
                ModelSelectorUiState.Item(
                    modelName = model.displayName,
                    selected = model.selectionKey == selectedModel.selectionKey,
                    listener = object : ModelSelectorUiState.ItemListener {
                        override fun onClick() {
                            onSelectModel(model.withThinking(selectedModel.thinkingEnabled))
                        }
                    },
                )
            },
            thinkingEnabled = selectedModel.thinkingEnabled,
            thinkingToggleEnabled = selectedModel.thinkingToggleEnabled,
            listener = object : ModelSelectorUiState.Listener {
                override fun onChangeThinking(enabled: Boolean) {
                    onSelectModel(selectedModel.withThinking(enabled))
                }
            },
        )
    }

    fun createSelectableModels(
        activeLocalModelKeys: Set<LocalModelId>,
        localModelDefs: List<LocalModelDefinition>,
    ): List<ChatGptModel> {
        return ChatGptModel.entries + activeLocalModelKeys.mapNotNull { key ->
            val def = localModelDefs.find { it.modelId == key } ?: return@mapNotNull null
            def.toChatGptModel()
        }
    }
}
