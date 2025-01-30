package net.matsudamper.gptclient.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.gptclient.MainScreenUiState
import net.matsudamper.gptclient.navigation.Navigator
import net.matsudamper.gptclient.room.AppDatabase
import net.matsudamper.gptclient.room.entity.ChatRoomId
import net.matsudamper.gptclient.room.entity.ChatRoomWithStartChat

class MainScreenViewModel(
    private val appDatabase: AppDatabase,
    private val navControllerProvider: () -> NavHostController,
) : ViewModel() {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())
    val uiStateFlow: StateFlow<MainScreenUiState> = MutableStateFlow(
        MainScreenUiState(
            history = MainScreenUiState.History.Loading,
            listener = object : MainScreenUiState.Listener {
                override fun onClickSettings() {
                    navControllerProvider().navigate(
                        Navigator.Settings
                    )
                }
            }
        )
    ).also { uiState ->
        viewModelScope.launch {
            appDatabase.chatRoomDao().getAllChatRoomWithStartChat().collectLatest { rooms ->
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        rooms = rooms
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
                                        text = room.textMessage.orEmpty(),
                                    )
                                }
                            )
                        }
                    )
                }
            }
        }
    }

    private inner class HistoryItemListenerImpl(val roomId: ChatRoomId) : MainScreenUiState.HistoryItemListener {
        override fun onClick() {
            val navHostController: NavHostController = navControllerProvider()
            navHostController.navigate(Navigator.Chat(Navigator.Chat.ChatOpenContext.OpenChat(roomId)))
        }
    }

    private data class ViewModelState(
        val rooms: List<ChatRoomWithStartChat>? = null,
    )
}
