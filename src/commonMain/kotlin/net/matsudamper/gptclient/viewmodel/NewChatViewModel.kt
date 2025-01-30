package net.matsudamper.gptclient.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.gptclient.navigation.Navigator
import net.matsudamper.gptclient.ui.NewChatUiState

class NewChatViewModel(
    navControllerProvider: () -> NavHostController,
) : ViewModel() {
    public val uiState: StateFlow<NewChatUiState> = MutableStateFlow(
        NewChatUiState(
            projects = listOf(),
            listener = object : NewChatUiState.Listener {
                override fun send(text: String) {
                    navControllerProvider().navigate(
                        Navigator.Chat(Navigator.Chat.ChatOpenContext.NewMessage(text))
                    )
                }
            }
        )
    ).also { uiState ->
        viewModelScope.launch {
            uiState.update {
                it
            }
        }
    }
}
