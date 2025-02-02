package net.matsudamper.gptclient.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.gptclient.MainScreenUiState
import net.matsudamper.gptclient.PlatformRequest
import net.matsudamper.gptclient.entity.getName
import net.matsudamper.gptclient.navigation.Navigator
import net.matsudamper.gptclient.room.AppDatabase
import net.matsudamper.gptclient.room.entity.ChatRoomId
import net.matsudamper.gptclient.room.entity.ChatRoomWithSummary
import net.matsudamper.gptclient.usecase.DeleteChatRoomUseCase

class MainScreenViewModel(
    private val appDatabase: AppDatabase,
    private val platformRequest: PlatformRequest,
    private val deleteChatRoomUseCase: DeleteChatRoomUseCase,
    private val navControllerProvider: () -> NavHostController,
) : ViewModel() {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())
    val uiStateFlow: StateFlow<MainScreenUiState> = MutableStateFlow(
        MainScreenUiState(
            history = MainScreenUiState.History.Loading,
            listener = object : MainScreenUiState.Listener {
                override fun onClickSettings() {
                    navControllerProvider().navigate(
                        Navigator.Settings,
                    )
                }

                override fun onClickUsage() {
                    platformRequest.openLink(
                        url = "https://platform.openai.com/settings/organization/usage",
                    )
                }

                override fun onClickHome() {
                    navControllerProvider().navigate(
                        Navigator.StartChat,
                    ) {
                        popUpTo(0) { inclusive = true }
                    }
                }

                override fun clearHistory() {
                    clearAllHistory()
                }
            },
        ),
    ).also { uiState ->
        viewModelScope.launch {
            appDatabase.chatRoomDao().getAllChatRoomWithStartChat(isAsc = false).collectLatest { rooms ->
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        rooms = rooms,
                    )
                }
            }
        }
        viewModelScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                uiState.update {
                    it.copy(
                        history = if (viewModelState.rooms == null) {
                            MainScreenUiState.History.Loading
                        } else {
                            MainScreenUiState.History.Loaded(
                                items = viewModelState.rooms.map { room ->
                                    MainScreenUiState.HistoryItem(
                                        listener = HistoryItemListenerImpl(room.chatRoom.id),
                                        projectName = room.projectName ?: room.chatRoom.builtInProjectId?.getName(),
                                        text = room.chatRoom.summary
                                            ?: room.textMessage?.replace("\n", "")?.takeIf { it.isNotBlank() }
                                            ?: "(空白)",
                                    )
                                },
                            )
                        },
                    )
                }
            }
        }
    }

    private inner class HistoryItemListenerImpl(val roomId: ChatRoomId) : MainScreenUiState.HistoryItemListener {
        override fun onClick() {
            val navHostController: NavHostController = navControllerProvider()
            navHostController.navigate(Navigator.Chat(Navigator.Chat.ChatOpenContext.OpenChat(roomId))) {
                popUpTo(navHostController.graph.startDestinationRoute!!) {
                    inclusive = false
                }
            }
        }
    }

    private fun clearAllHistory() {
        viewModelScope.launch {
            val allChatRooms = appDatabase.chatRoomDao().getAll().first()

            for (chatRoom in allChatRooms) {
                deleteChatRoomUseCase.deleteChatRoom(
                    chatRoomId = chatRoom.id,
                )
            }
        }
    }

    private data class ViewModelState(
        val rooms: List<ChatRoomWithSummary>? = null,
    )
}
