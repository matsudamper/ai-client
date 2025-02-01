package net.matsudamper.gptclient.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.matsudamper.gptclient.PlatformRequest
import net.matsudamper.gptclient.gpt.ChatGptClient
import net.matsudamper.gptclient.navigation.Navigator
import net.matsudamper.gptclient.room.AppDatabase
import net.matsudamper.gptclient.room.entity.BuiltinProjectId
import net.matsudamper.gptclient.room.entity.Chat
import net.matsudamper.gptclient.room.entity.ChatRoom
import net.matsudamper.gptclient.room.entity.ChatRoomId
import net.matsudamper.gptclient.ui.ChatListUiState

class ChatViewModel(
    openContext: Navigator.Chat.ChatOpenContext,
    private val platformRequest: PlatformRequest,
    private val appDatabase: AppDatabase,
    private val insertDataAndAddRequestUseCase: AddRequestUseCase,
) : ViewModel() {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())
    private val listener = object : ChatListUiState.Listener {
        override fun onClickImage() {
            viewModelScope.launch {
                try {
                    viewModelStateFlow.update {
                        it.copy(isMediaLoading = true)
                    }
                    val media = platformRequest.getMedia()
                    viewModelStateFlow.update {
                        it.copy(
                            selectedMedia = media,
                        )
                    }
                } finally {
                    viewModelStateFlow.update {
                        it.copy(isMediaLoading = false)
                    }
                }
            }
        }

        override fun onClickVoice() {

        }

        override fun onClickSend(text: String) {
            addRequest(
                message = text,
                uris = viewModelStateFlow.value.selectedMedia,
            )
            viewModelStateFlow.update {
                it.copy(selectedMedia = listOf())
            }
        }
    }
    val uiStateFlow: StateFlow<ChatListUiState> = MutableStateFlow(
        ChatListUiState(
            items = listOf(),
            selectedMedia = listOf(),
            visibleMediaLoading = false,
            listener = listener,
        )
    ).also { uiState ->
        viewModelScope.launch {
            when (openContext) {
                is Navigator.Chat.ChatOpenContext.NewMessage -> {
                    val room = createRoom(builtinProjectId = null)

                    viewModelStateFlow.update {
                        it.copy(room = room)
                    }
                    addRequest(
                        message = openContext.initialMessage,
                        uris = openContext.uriList,
                    )
                }

                is Navigator.Chat.ChatOpenContext.OpenChat -> {
                    restoreChatRoom(openContext.chatRoomId)
                }
            }
        }
        viewModelScope.launch {
            viewModelStateFlow.map { it.room?.id }
                .filterNotNull()
                .stateIn(this)
                .collectLatest { roomId ->
                    appDatabase.chatDao().get(chatRoomId = roomId.value)
                        .collectLatest { chats ->
                            viewModelStateFlow.update { viewModelState ->
                                viewModelState.copy(
                                    chats = chats
                                )
                            }
                        }
                }
        }
        viewModelScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                uiState.update {
                    it.copy(
                        selectedMedia = viewModelState.selectedMedia,
                        visibleMediaLoading = viewModelState.isMediaLoading,
                        items = CreateChatMessageUiStateUseCase().create(
                            chats = viewModelState.chats,
                            isChatLoading = viewModelState.isChatLoading,
                        ),
                    )
                }
            }
        }
    }

    private fun restoreChatRoom(chatRoomId: ChatRoomId) {
        viewModelScope.launch {
            val room = appDatabase.chatRoomDao().get(chatRoomId = chatRoomId.value)
                .first()
            viewModelStateFlow.update {
                it.copy(
                    room = room,
                )
            }
        }
    }

    private suspend fun createRoom(builtinProjectId: BuiltinProjectId?): ChatRoom {
        return withContext(Dispatchers.IO) {
            val room = ChatRoom(
                modelName = "gpt-4o-mini", // TODO SELECT
                builtInProjectId = builtinProjectId,
            )
            room.copy(
                id = ChatRoomId(appDatabase.chatRoomDao().insert(room))
            )
        }
    }

    private fun addRequest(message: String, uris: List<String> = listOf()) {
        if (message.isEmpty() && uris.isEmpty()) return
        val chatRoomId = viewModelStateFlow.value.room?.id ?: return

        viewModelScope.launch {
            try {
                viewModelStateFlow.update {
                    it.copy(isChatLoading = true)
                }
                insertDataAndAddRequestUseCase.add(
                    chatRoomId = chatRoomId,
                    message = message,
                    uris = uris,
                    systemMessage = null,
                    format = ChatGptClient.Format.Text,
                )
            }finally {
                viewModelStateFlow.update {
                    it.copy(isChatLoading = false)
                }
            }
        }
    }

    private data class ViewModelState(
        val room: ChatRoom? = null,
        val chats: List<Chat> = listOf(),
        val selectedMedia: List<String> = listOf(),
        val isMediaLoading: Boolean = false,
        val isChatLoading: Boolean = false,
    )
}
