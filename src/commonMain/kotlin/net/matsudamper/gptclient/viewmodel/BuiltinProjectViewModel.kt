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
import net.matsudamper.gptclient.room.AppDatabase
import net.matsudamper.gptclient.room.entity.BuiltinProjectId
import net.matsudamper.gptclient.room.entity.ChatRoomId
import net.matsudamper.gptclient.room.entity.ChatRoomWithSummary
import net.matsudamper.gptclient.ui.BuiltinProjectUiState

class BuiltinProjectViewModel(
    private val navigator: Navigator.BuiltinProject,
    private val platformRequest: PlatformRequest,
    private val appDatabase: AppDatabase,
    private val navControllerProvider: () -> NavHostController
) : ViewModel() {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())
    val uiStateFlow: StateFlow<BuiltinProjectUiState> = MutableStateFlow(
        BuiltinProjectUiState(
            projectName = navigator.title,
            chatRoomsState = BuiltinProjectUiState.ChatRoomsState.Loading,
            visibleMediaLoading = false,
            selectedMedia = listOf(),
            systemMessage = BuiltinProjectUiState.SystemMessage(
                text = "",
                editable = false,
            ),
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
                    when(navigator.builtinProjectId) {
                        BuiltinProjectId.Calendar -> {
                            navControllerProvider().navigate(
                                Navigator.CalendarChat(
                                    openContext = Navigator.CalendarChat.ChatOpenContext.NewMessage(
                                        initialMessage = text,
                                        uriList = viewModelStateFlow.value.uriList,
                                    )
                                )
                            )
                        }
                        else -> TODO()
                    }
                }
            }
        )
    ).also { uiStateFlow ->
        viewModelScope.launch {
            appDatabase.chatRoomDao().getFromBuiltInChatRoomId(
                builtInChatRoomId = navigator.builtinProjectId.id,
                isAsc = false,
            ).collectLatest { chatRooms ->
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        chatRooms = chatRooms,
                    )
                }
            }
        }
        viewModelScope.launch {
            val systemInfo = GetBuiltinProjectInfoUseCase().exec(navigator.builtinProjectId)
            viewModelStateFlow.update {
                it.copy(
                    systemInfo = systemInfo,
                )
            }
        }
        viewModelScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        systemMessage = BuiltinProjectUiState.SystemMessage(
                            text = viewModelState.systemInfo?.systemMessage.orEmpty(),
                            editable = false,
                        ),
                        selectedMedia = viewModelState.uriList,
                        visibleMediaLoading = viewModelState.mediaLoading,
                        chatRoomsState = run rooms@{
                            val chatRooms = viewModelState.chatRooms
                                ?: return@rooms BuiltinProjectUiState.ChatRoomsState.Loading

                            BuiltinProjectUiState.ChatRoomsState.Loaded(
                                histories = chatRooms.map { room ->
                                    BuiltinProjectUiState.History(
                                        text = room.textMessage ?: "空白",
                                        listener = ChatRoomListener(
                                            chatRoomId = room.chatRoom.id,
                                        ),
                                    )
                                }
                            )
                        }
                    )
                }
            }
        }
    }

    inner class ChatRoomListener(private val chatRoomId: ChatRoomId) : BuiltinProjectUiState.History.Listener {
        override fun onClick() {
            navControllerProvider().navigate(
                Navigator.CalendarChat(
                    openContext = Navigator.CalendarChat.ChatOpenContext.OpenChat(
                        chatRoomId = chatRoomId,
                    )
                )
            )
        }
    }

    private data class ViewModelState(
        val uriList: List<String> = listOf(),
        val mediaLoading: Boolean = false,
        val chatRooms: List<ChatRoomWithSummary>? = null,
        val systemInfo: GetBuiltinProjectInfoUseCase.Info? = null,
    )
}
