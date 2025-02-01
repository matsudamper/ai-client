package net.matsudamper.gptclient.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.gptclient.PlatformRequest
import net.matsudamper.gptclient.entity.Calendar
import net.matsudamper.gptclient.navigation.Navigator
import net.matsudamper.gptclient.room.entity.BuiltinProjectId
import net.matsudamper.gptclient.ui.NewChatUiState

class NewChatViewModel(
    private val platformRequest: PlatformRequest,
    navControllerProvider: () -> NavHostController,
) : ViewModel() {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())
    public val uiState: StateFlow<NewChatUiState> = MutableStateFlow(
        NewChatUiState(
            projects = listOf(
                NewChatUiState.Project(
                    name = "カレンダー追加",
                    listener = object : NewChatUiState.Project.Listener {
                        override fun onClick() {
                            navControllerProvider().navigate(
                                Navigator.BuiltinProject(
                                    title = "カレンダー追加",
                                    builtinProjectId = BuiltinProjectId.Calendar,
                                )
                            )
                        }
                    },
                )
            ),
            selectedMedia = listOf(),
            visibleMediaLoading = false,
            listener = object : NewChatUiState.Listener {
                override fun send(text: String) {
                    navControllerProvider().navigate(
                        Navigator.Chat(
                            Navigator.Chat.ChatOpenContext.NewMessage(
                                initialMessage = text,
                                uriList = viewModelStateFlow.value.mediaList,
                            )
                        )
                    )
                }

                override fun onClickSelectMedia() {
                    viewModelScope.launch {
                        try {
                            viewModelStateFlow.update {
                                it.copy(mediaLoading = true)
                            }
                            val media = platformRequest.getMedia()
                            viewModelStateFlow.update {
                                it.copy(mediaList = media)
                            }
                        } finally {
                            viewModelStateFlow.update {
                                it.copy(mediaLoading = false)
                            }
                        }
                    }
                }

                override fun onClickVoice() {

                }
            }
        )
    ).also { uiState ->
        viewModelScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                uiState.update {
                    it.copy(
                        selectedMedia = viewModelState.mediaList,
                        visibleMediaLoading = viewModelState.mediaLoading,
                    )
                }
            }
        }
    }

    private data class ViewModelState(
        val mediaList: List<String> = listOf(),
        val mediaLoading: Boolean = false,
    )
}
