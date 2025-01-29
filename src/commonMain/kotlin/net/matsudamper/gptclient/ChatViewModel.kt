package net.matsudamper.gptclient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.matsudamper.gptclient.ui.ChatListUiState

class ChatViewModel(
    initialMessage: String,
    navControllerProvider: () -> NavController,
) : ViewModel() {
    val uiStateFlow: StateFlow<ChatListUiState> = MutableStateFlow(
        ChatListUiState(
            items = listOf(
                ChatListUiState.Item.User(initialMessage)
            ),
            listener = object : ChatListUiState.Listener {
                override fun onClickImage() {
                }

                override fun onClickVoice() {

                }
            }
        )
    ).also { uiState ->
        viewModelScope.launch {
            initialMessage
        }
    }
}