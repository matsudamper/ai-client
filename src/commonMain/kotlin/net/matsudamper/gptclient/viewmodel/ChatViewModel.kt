package net.matsudamper.gptclient.viewmodel

import androidx.compose.ui.text.AnnotatedString
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
import net.matsudamper.gptclient.entity.ChatGptModel
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
            errorDialogMessage = null,
        )
    ).also { uiState ->
        viewModelScope.launch {
            viewModelStateFlow.map { it.roomInfo?.room?.id }
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
                            agentTransformer = {
                                when(val info = viewModelState.roomInfo) {
                                    is ViewModelState.RoomInfo.BuiltinProject -> {
                                        info.builtinProjectInfo.responseTransformer(it)
                                    }
                                    is ViewModelState.RoomInfo.Normal,
                                    null -> AnnotatedString(it)
                                }
                            }
                        ),
                    )
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            when (openContext) {
                is Navigator.Chat.ChatOpenContext.NewMessage -> {
                    val room = createRoom(
                        builtinProjectId = when (val chatType = openContext.chatType) {
                            is Navigator.Chat.ChatType.Builtin -> chatType.builtinProjectId
                            is Navigator.Chat.ChatType.Normal -> null
                        }
                    )

                    viewModelStateFlow.update {
                        it.copy(
                            roomInfo = when (val chatType = openContext.chatType) {
                                is Navigator.Chat.ChatType.Builtin -> {
                                    ViewModelState.RoomInfo.BuiltinProject(
                                        room = room,
                                        builtinProjectId = chatType.builtinProjectId,
                                        builtinProjectInfo = GetBuiltinProjectInfoUseCase().exec(
                                            chatType.builtinProjectId,
                                        )
                                    )
                                }

                                is Navigator.Chat.ChatType.Normal -> {
                                    ViewModelState.RoomInfo.Normal(room)
                                }
                            },
                        )
                    }

                    addRequest(
                        message = openContext.initialMessage,
                        uris = openContext.uriList,
                    )
                }

                is Navigator.Chat.ChatOpenContext.OpenChat -> {
                    val room = appDatabase.chatRoomDao()
                        .get(chatRoomId = openContext.chatRoomId.value)
                        .first()
                    val builtInProjectId = room.builtInProjectId
                    viewModelStateFlow.update {
                        it.copy(
                            roomInfo = if (builtInProjectId != null) {
                                ViewModelState.RoomInfo.BuiltinProject(
                                    room = room,
                                    builtinProjectId = builtInProjectId,
                                    builtinProjectInfo = GetBuiltinProjectInfoUseCase().exec(
                                        builtInProjectId,
                                    )
                                )
                            } else {
                                ViewModelState.RoomInfo.Normal(room = room)
                            },
                        )
                    }
                }
            }
        }
    }

    private suspend fun createRoom(builtinProjectId: BuiltinProjectId?): ChatRoom {
        return withContext(Dispatchers.IO) {
            val room = ChatRoom(
                modelName = ChatGptModel.Gpt4oMini.modelName, // TODO SELECT
                builtInProjectId = builtinProjectId,
            )
            room.copy(
                id = ChatRoomId(appDatabase.chatRoomDao().insert(room))
            )
        }
    }

    private fun addRequest(message: String, uris: List<String> = listOf()) {
        if (message.isEmpty() && uris.isEmpty()) return
        val roomInfo = viewModelStateFlow.value.roomInfo ?: return
        val chatRoomId = roomInfo.room.id

        viewModelScope.launch {
            try {
                viewModelStateFlow.update {
                    it.copy(isChatLoading = true)
                }
                val result = insertDataAndAddRequestUseCase.add(
                    chatRoomId = chatRoomId,
                    message = message,
                    uris = uris,
                    systemMessage = when(roomInfo) {
                        is ViewModelState.RoomInfo.BuiltinProject -> roomInfo.builtinProjectInfo.systemMessage
                        is ViewModelState.RoomInfo.Normal -> null
                    },
                    format = ChatGptClient.Format.Text,
                )
                when (result) {
                    is AddRequestUseCase.Result.Success -> Unit
                    is AddRequestUseCase.Result.InputError -> throw IllegalArgumentException()
                    is AddRequestUseCase.Result.GptResultError -> when (result.gptError.reason) {
                        is ChatGptClient.GptResult.ErrorReason.ImageNotSupported -> {
                            platformRequest.showToast(result.gptError.reason.message)
                        }

                        is ChatGptClient.GptResult.ErrorReason.Unknown -> {
                            viewModelStateFlow.update {
                                it.copy(
                                    errorDialogMessage = result.gptError.reason.message,
                                )
                            }
                        }
                    }
                }
            } finally {
                viewModelStateFlow.update {
                    it.copy(isChatLoading = false)
                }
            }
        }
    }

    private data class ViewModelState(
        val roomInfo: RoomInfo? = null,
        val chats: List<Chat> = listOf(),
        val selectedMedia: List<String> = listOf(),
        val isMediaLoading: Boolean = false,
        val isChatLoading: Boolean = false,
        val errorDialogMessage: String? = null,
    ) {
        sealed interface RoomInfo {
            val room: ChatRoom

            data class Normal(override val room: ChatRoom) : RoomInfo
            data class BuiltinProject(
                override val room: ChatRoom,
                val builtinProjectId: BuiltinProjectId,
                val builtinProjectInfo: GetBuiltinProjectInfoUseCase.Info,
            ) : RoomInfo
        }
    }
}
