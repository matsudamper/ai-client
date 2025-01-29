package net.matsudamper.gptclient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.gptclient.datastore.SettingDataStore
import net.matsudamper.gptclient.gpt.ChatGptClient
import net.matsudamper.gptclient.ui.ChatListUiState

class ChatViewModel(
    initialMessage: String? = null,
    private val settingDataStore: SettingDataStore,
    private val navControllerProvider: () -> NavController,
) : ViewModel() {
    val uiStateFlow: StateFlow<ChatListUiState> = MutableStateFlow(
        ChatListUiState(
            items = buildList {
                if (initialMessage != null) {
                    add(ChatListUiState.Item.User(initialMessage))
                }
            },
            listener = object : ChatListUiState.Listener {
                override fun onClickImage() {
                }

                override fun onClickVoice() {

                }
            }
        )
    ).also { uiState ->
        viewModelScope.launch {
            if (initialMessage != null) {
                val response = getGptClient().request(
                    messages = listOf(
                        ChatGptClient.GptMessage(
                            role = ChatGptClient.GptMessage.Role.User,
                            contents = listOf(
                                ChatGptClient.GptMessage.Content.Text(initialMessage)
                            )
                        )
                    )
                )
                uiState.update {
                    val message = response.choices.last().message
                    it.copy(
                        items = it.items.toMutableList().also {
                            it.add(
                                ChatListUiState.Item.Agent(message.content)
                            )
                        }
                    )
                }
            }
        }
    }

    private suspend fun getGptClient(): ChatGptClient {
        return ChatGptClient(settingDataStore.getSecretKey())
    }
}