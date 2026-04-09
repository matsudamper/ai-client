package net.matsudamper.gptclient.viewmodel

import net.matsudamper.gptclient.entity.ChatGptModel
import net.matsudamper.gptclient.localmodel.LocalModelDefinition
import net.matsudamper.gptclient.localmodel.LocalModelId
import net.matsudamper.gptclient.localmodel.toChatGptModel
import net.matsudamper.gptclient.ui.component.ModelSelectorUiState

internal object ModelSelectorStateFactory {
    fun create(
        selectedModel: ChatGptModel?,
        activeLocalModelKeys: Set<LocalModelId>,
        localModelDefs: List<LocalModelDefinition>,
        geminiBillingKeyOverrideSelectionKeys: Set<String>,
        onSelectModel: (ChatGptModel) -> Unit,
        onChangeGeminiBillingKey: (selectionKey: String, enabled: Boolean) -> Unit,
    ): ModelSelectorUiState {
        val selectableModels = createSelectableModels(
            activeLocalModelKeys = activeLocalModelKeys,
            localModelDefs = localModelDefs,
        )

        return ModelSelectorUiState(
            selectedModelName = selectedModel?.displayName ?: "モデルを選択",
            items = selectableModels.map { model ->
                ModelSelectorUiState.Item(
                    modelName = model.displayName,
                    selected = model.selectionKey == selectedModel?.selectionKey,
                    listener = object : ModelSelectorUiState.ItemListener {
                        override fun onClick() {
                            onSelectModel(model.withThinking(selectedModel?.thinkingEnabled == true))
                        }
                    },
                )
            },
            thinkingEnabled = selectedModel?.thinkingEnabled ?: false,
            thinkingToggleEnabled = selectedModel?.thinkingToggleEnabled ?: false,
            overflowMenu = createOverflowMenu(
                selectedModel = selectedModel,
                geminiBillingKeyOverrideSelectionKeys = geminiBillingKeyOverrideSelectionKeys,
                onChangeGeminiBillingKey = onChangeGeminiBillingKey,
            ),
            listener = object : ModelSelectorUiState.Listener {
                override fun onChangeThinking(enabled: Boolean) {
                    selectedModel ?: return
                    onSelectModel(selectedModel.withThinking(enabled))
                }
            },
        )
    }

    private fun createOverflowMenu(
        selectedModel: ChatGptModel?,
        geminiBillingKeyOverrideSelectionKeys: Set<String>,
        onChangeGeminiBillingKey: (selectionKey: String, enabled: Boolean) -> Unit,
    ): ModelSelectorUiState.OverflowMenu {
        if (selectedModel !is ChatGptModel.Remote.Gemini) {
            return ModelSelectorUiState.OverflowMenu.None
        }
        val selectionKey = selectedModel.selectionKey
        val requireBilling = selectedModel.requireBillingKey
        return ModelSelectorUiState.OverflowMenu.Gemini(
            billingKeyEnabled = requireBilling || selectionKey in geminiBillingKeyOverrideSelectionKeys,
            billingKeyToggleEnabled = !requireBilling,
            onChangeBillingKey = { enabled ->
                onChangeGeminiBillingKey(selectionKey, enabled)
            },
        )
    }

    fun createSelectableModels(
        activeLocalModelKeys: Set<LocalModelId>,
        localModelDefs: List<LocalModelDefinition>,
    ): List<ChatGptModel> {
        return ChatGptModel.entries + activeLocalModelKeys.mapNotNull { key ->
            val def = localModelDefs.find { it.modelId == key } ?: return@mapNotNull null
            def.toChatGptModel(modelKey = key.value)
        }
    }
}
