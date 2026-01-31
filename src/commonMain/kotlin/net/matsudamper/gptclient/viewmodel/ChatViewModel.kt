package net.matsudamper.gptclient.viewmodel

import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
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
import net.matsudamper.gptclient.entity.getName
import net.matsudamper.gptclient.navigation.Navigator
import net.matsudamper.gptclient.room.AppDatabase
import net.matsudamper.gptclient.room.entity.BuiltinProjectId
import net.matsudamper.gptclient.room.entity.Chat
import net.matsudamper.gptclient.room.entity.ChatRoom
import net.matsudamper.gptclient.room.entity.ChatRoomId
import net.matsudamper.gptclient.room.entity.ProjectId
import net.matsudamper.gptclient.ui.ChatListUiState
import net.matsudamper.gptclient.ui.chat.TextMessageComposableInterface
import net.matsudamper.gptclient.ui.component.ChatFooterImage

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
                    val images = platformRequest.getMediaList()
                    viewModelStateFlow.update { viewModelState ->
                        viewModelState.copy(
                            selectedMedia = images.map { image ->
                                ChatFooterImage(
                                    imageUri = image,
                                    rect = null,
                                    listener = ChatFooterImageListener(image),
                                )
                            },
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
            viewModelScope.launch {
                addRequest(
                    message = text,
                    uris = viewModelStateFlow.value.selectedMedia.mapNotNull map@{
                        val rect = it.rect ?: return@map it.imageUri
                        val platformCropRect = PlatformRequest.CropRect(
                            left = rect.left,
                            top = rect.top,
                            right = rect.right,
                            bottom = rect.bottom,
                        )

                        platformRequest.cropImage(
                            uri = it.imageUri,
                            cropRect = platformCropRect,
                        )
                    },
                )
            }
            viewModelStateFlow.update {
                it.copy(selectedMedia = listOf())
            }
        }

        override fun onClickRetry() {
            retryRequest()
        }
    }
    val uiStateFlow: StateFlow<ChatListUiState> = MutableStateFlow(
        ChatListUiState(
            items = listOf(),
            selectedImage = listOf(),
            visibleMediaLoading = false,
            listener = listener,
            errorDialogMessage = null,
            modelLoadingState = ChatListUiState.ModelLoadingState.Loading,
            title = "",
            enableSend = false,
        ),
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
                                    chats = chats,
                                )
                            }
                        }
                }
        }
        viewModelScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                uiState.update {
                    it.copy(
                        title = when (val roomInfo = viewModelState.roomInfo) {
                            is ViewModelState.RoomInfo.BuiltinProject -> {
                                roomInfo.builtinProjectId.getName()
                            }

                            is ViewModelState.RoomInfo.Project -> {
                                roomInfo.project.name
                            }

                            is ViewModelState.RoomInfo.Normal -> ""
                            null -> ""
                        },
                        modelLoadingState = run {
                            val roomInfo = viewModelState.roomInfo ?: return@run ChatListUiState.ModelLoadingState.Loading
                            ChatListUiState.ModelLoadingState.Loaded(
                                ChatGptModel.entries.map { model ->
                                    ChatListUiState.Model(
                                        modelName = model.modelName,
                                        selected = model.modelName == roomInfo.room.modelName,
                                        listener = object : ChatListUiState.Model.Listener {
                                            override fun onClick() {
                                                viewModelScope.launch {
                                                    appDatabase.chatRoomDao().update(
                                                        roomInfo.room.copy(
                                                            modelName = model.modelName,
                                                        ),
                                                    )
                                                }
                                            }
                                        },
                                    )
                                },
                            )
                        },
                        selectedImage = viewModelState.selectedMedia,
                        visibleMediaLoading = viewModelState.isMediaLoading,
                        enableSend = !viewModelState.isChatLoading && !viewModelState.isWorkInProgress && !viewModelState.isMediaLoading,
                        items = CreateChatMessageUiStateUseCase().create(
                            chats = viewModelState.chats,
                            isChatLoading = viewModelState.isWorkInProgress,
                            agentTransformer = {
                                when (val info = viewModelState.roomInfo) {
                                    is ViewModelState.RoomInfo.BuiltinProject -> {
                                        info.builtinProjectInfo.responseTransformer(it)
                                    }

                                    is ViewModelState.RoomInfo.Project,
                                    is ViewModelState.RoomInfo.Normal,
                                    null,
                                    -> TextMessageComposableInterface(AnnotatedString(it))
                                }
                            },
                        ),
                        errorDialogMessage = viewModelState.errorDialogMessage,
                    )
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            viewModelStateFlow.map { viewModelState ->
                viewModelState.roomInfo?.room?.id
            }.filterNotNull().stateIn(this).collectLatest { roomId ->
                appDatabase.chatRoomDao().get(chatRoomId = roomId.value).collectLatest { room ->
                    val isWorkInProgress = insertDataAndAddRequestUseCase.isWorkInProgress(roomId)
                    viewModelStateFlow.update {
                        it.copy(
                            roomInfo = it.roomInfo?.copyOnlyRoom(room),
                            isWorkInProgress = isWorkInProgress,
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            when (openContext) {
                is Navigator.Chat.ChatOpenContext.NewMessage -> {
                    val room = createRoom(
                        builtinProjectId = when (val chatType = openContext.chatType) {
                            is Navigator.Chat.ChatType.BuiltinProject -> chatType.builtinProjectId
                            else -> null
                        },
                        projectId = when (val chatType = openContext.chatType) {
                            is Navigator.Chat.ChatType.Project -> chatType.projectId
                            is Navigator.Chat.ChatType.BuiltinProject,
                            is Navigator.Chat.ChatType.Normal,
                            -> null
                        },
                        model = openContext.model,
                    )

                    val roomInfo = when (val chatType = openContext.chatType) {
                        is Navigator.Chat.ChatType.BuiltinProject -> {
                            ViewModelState.RoomInfo.BuiltinProject(
                                room = room,
                                builtinProjectId = chatType.builtinProjectId,
                                builtinProjectInfo = GetBuiltinProjectInfoUseCase().exec(
                                    chatType.builtinProjectId,
                                    platformRequest = platformRequest,
                                ),
                            )
                        }

                        is Navigator.Chat.ChatType.Normal -> {
                            ViewModelState.RoomInfo.Normal(room)
                        }

                        is Navigator.Chat.ChatType.Project -> {
                            val project = appDatabase.projectDao().get(projectId = chatType.projectId.id).first()
                            ViewModelState.RoomInfo.Project(
                                project = project ?: return@launch,
                                room = room,
                                projectId = chatType.projectId,
                            )
                        }
                    }
                    viewModelStateFlow.update {
                        it.copy(roomInfo = roomInfo)
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
                                        platformRequest = platformRequest,
                                    ),
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

    private fun retryRequest() {
        val roomInfo = viewModelStateFlow.value.roomInfo ?: return
        val chatRoomId = roomInfo.room.id

        viewModelScope.launch {
            try {
                viewModelStateFlow.update {
                    it.copy(isWorkInProgress = true)
                }

                val result = insertDataAndAddRequestUseCase.retryRequest(chatRoomId)
                when (result) {
                    is AddRequestUseCase.Result.Success,
                    is AddRequestUseCase.Result.WorkInProgress,
                    is AddRequestUseCase.Result.IsLastUserChat,
                    -> Unit

                    is AddRequestUseCase.Result.GptResultError -> {
                        platformRequest.showToast(result.gptError.reason.message)
                    }

                    is AddRequestUseCase.Result.ModelNotFoundError -> {
                        platformRequest.showToast("モデルが見つかりません")
                    }
                }
            } catch (_: Throwable) {
                platformRequest.showToast("エラー")
            } finally {
                viewModelStateFlow.update {
                    it.copy(isWorkInProgress = false)
                }
            }
        }
    }

    private suspend fun createRoom(
        builtinProjectId: BuiltinProjectId?,
        projectId: ProjectId?,
        model: ChatGptModel,
    ): ChatRoom = withContext(Dispatchers.IO) {
        val room = ChatRoom(
            modelName = model.modelName,
            builtInProjectId = builtinProjectId,
            projectId = projectId,
            summary = null,
        )
        room.copy(
            id = ChatRoomId(appDatabase.chatRoomDao().insert(room)),
        )
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
                val result = insertDataAndAddRequestUseCase.addRequest(
                    chatRoomId = chatRoomId,
                    message = message,
                    uris = uris,
                )
                when (result) {
                    is AddRequestUseCase.Result.Success,
                    is AddRequestUseCase.Result.IsLastUserChat,
                    is AddRequestUseCase.Result.WorkInProgress,
                    -> Unit

                    is AddRequestUseCase.Result.GptResultError,
                    -> {
                        platformRequest.showToast("エラーが発生しました")
                    }

                    AddRequestUseCase.Result.ModelNotFoundError -> {
                        platformRequest.showToast("モデル: ${roomInfo.room.modelName}がありません")
                    }
                }
            } finally {
                viewModelStateFlow.update {
                    it.copy(isChatLoading = false)
                }
            }
        }
    }

    private inner class ChatFooterImageListener(val imageUri: String) : ChatFooterImage.Listener {
        override fun crop(rect: ChatFooterImage.Rect) {
            viewModelStateFlow.update { state ->
                state.copy(
                    selectedMedia = state.selectedMedia.map { viewModelStateImage ->
                        if (imageUri == viewModelStateImage.imageUri) {
                            viewModelStateImage.copy(
                                rect = ChatFooterImage.Rect(
                                    left = rect.left,
                                    top = rect.top,
                                    right = rect.right,
                                    bottom = rect.bottom,
                                ),
                            )
                        } else {
                            viewModelStateImage
                        }
                    },
                )
            }
        }
    }

    private data class ViewModelState(
        val roomInfo: RoomInfo? = null,
        val chats: List<Chat> = listOf(),
        val selectedMedia: List<ChatFooterImage> = listOf(),
        val isMediaLoading: Boolean = false,
        val isChatLoading: Boolean = false,
        val isWorkInProgress: Boolean = false,
        val errorDialogMessage: String? = null,
    ) {
        sealed interface RoomInfo {
            val room: ChatRoom

            data class Normal(override val room: ChatRoom) : RoomInfo
            data class BuiltinProject(override val room: ChatRoom, val builtinProjectId: BuiltinProjectId, val builtinProjectInfo: GetBuiltinProjectInfoUseCase.Info) : RoomInfo

            data class Project(override val room: ChatRoom, val projectId: ProjectId, val project: net.matsudamper.gptclient.room.entity.Project) : RoomInfo

            fun copyOnlyRoom(room: ChatRoom): RoomInfo = when (this) {
                is BuiltinProject -> copy(room = room)
                is Normal -> copy(room = room)
                is Project -> copy(room = room)
            }
        }
    }
}
