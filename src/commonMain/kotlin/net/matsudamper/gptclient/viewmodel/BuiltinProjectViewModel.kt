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
import net.matsudamper.gptclient.navigation.Navigator
import net.matsudamper.gptclient.ui.BuiltinProjectUiState

class BuiltinProjectViewModel(
    private val navigator: Navigator.BuiltinProject,
    private val platformRequest: PlatformRequest,
    private val navControllerProvider: () -> NavHostController
) : ViewModel() {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())
    val uiStateFlow: StateFlow<BuiltinProjectUiState> = MutableStateFlow(
        BuiltinProjectUiState(
            projectName = navigator.title,
            state = BuiltinProjectUiState.LoadingState.Loading,
            visibleMediaLoading = false,
            selectedMedia = listOf(),
            listener = object : BuiltinProjectUiState.Listener {
                override fun recordVoice() {

                }

                override fun selectMedia() {
                    viewModelScope.launch {
                        try {
                            viewModelStateFlow.update {
                                it.copy(mediaLoading = true)
                            }
                            val uriList = platformRequest.getMedia()
                            viewModelStateFlow.update {
                                it.copy(uriList = uriList)
                            }
                        } finally {
                            viewModelStateFlow.update {
                                it.copy(mediaLoading = false)
                            }
                        }
                    }
                }

                override fun send(text: String) {
                    navControllerProvider().navigate(
                        Navigator.Chat(
                            openContext = Navigator.Chat.ChatOpenContext.NewBuiltinMessage(
                                initialMessage = text,
                                uriList = viewModelStateFlow.value.uriList,
                                builtinProjectId = navigator.builtinProjectId,
                            )
                        )
                    )
                }
            }
        )
    ).also { uiState ->
        viewModelScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                uiState.update {
                    it.copy(
                        selectedMedia = viewModelState.uriList,
                        visibleMediaLoading = viewModelState.mediaLoading
                    )
                }
            }
        }
    }

    private data class ViewModelState(
        val uriList: List<String> = listOf(),
        val mediaLoading: Boolean = false,
    )
}
