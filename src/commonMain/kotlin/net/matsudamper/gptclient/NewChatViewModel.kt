package net.matsudamper.gptclient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NewChatViewModel : ViewModel() {
    public val uiState: StateFlow<NewChatUiState> = MutableStateFlow(
        NewChatUiState(
            projects = listOf(),
            listener = object : NewChatUiState.Listener {
                override fun send(text: String) {

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
