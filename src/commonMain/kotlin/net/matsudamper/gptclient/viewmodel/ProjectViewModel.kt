package net.matsudamper.gptclient.viewmodel

import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.gptclient.PlatformRequest
import net.matsudamper.gptclient.entity.ChatGptModel
import net.matsudamper.gptclient.gpt.ChatGptClient
import net.matsudamper.gptclient.navigation.Navigator
import net.matsudamper.gptclient.room.AppDatabase
import net.matsudamper.gptclient.room.entity.ChatRoomId
import net.matsudamper.gptclient.room.entity.ChatRoomWithSummary
import net.matsudamper.gptclient.ui.BuiltinProjectUiState

class ProjectViewModel(
    private val navigator: Navigator.Project,
    private val platformRequest: PlatformRequest,
    private val appDatabase: AppDatabase,
    private val navControllerProvider: () -> NavHostController,
) : ViewModel() {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())
    private val systemMessageListener = object : BuiltinProjectUiState.SystemMessage.Listener {
        override fun onChange(text: String) {
            when (val info = viewModelStateFlow.value.systemInfo ?: return) {
                is ViewModelState.SystemInfoType.BuiltinInfo -> Unit
                is ViewModelState.SystemInfoType.Project -> {
                    viewModelScope.launch {
                        appDatabase.projectDao().update(
                            info.project.copy(
                                systemMessage = text,
                            ),
                        )
                    }
                }
            }
        }
    }
    val uiStateFlow: StateFlow<BuiltinProjectUiState> = MutableStateFlow(
        BuiltinProjectUiState(
            projectName = navigator.title,
            chatRoomsState = BuiltinProjectUiState.ChatRoomsState.Loading,
            visibleMediaLoading = false,
            selectedMedia = listOf(),
            systemMessage = BuiltinProjectUiState.SystemMessage(
                text = "",
                editable = false,
                listener = systemMessageListener,
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
                    val systemInfo = viewModelStateFlow.value.systemInfo ?: return
                    val chatType = when (navigator.type) {
                        is Navigator.Project.ProjectType.Builtin -> {
                            Navigator.Chat.ChatType.Builtin(
                                navigator.type.builtinProjectId,
                            )
                        }

                        is Navigator.Project.ProjectType.Project -> {
                            Navigator.Chat.ChatType.Project(
                                navigator.type.projectId,
                            )
                        }
                    }
                    navControllerProvider().navigate(
                        Navigator.Chat(
                            openContext = Navigator.Chat.ChatOpenContext.NewMessage(
                                initialMessage = text,
                                uriList = viewModelStateFlow.value.uriList,
                                chatType = chatType,
                                model = systemInfo.getInfo().model,
                            ),
                        ),
                    )

                    viewModelStateFlow.update {
                        it.copy(uriList = listOf())
                    }
                }
            },
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            when (navigator.type) {
                is Navigator.Project.ProjectType.Builtin -> {
                    appDatabase.chatRoomDao().getFromBuiltInChatRoomId(
                        builtInChatRoomId = navigator.type.builtinProjectId.id,
                        isAsc = false,
                    ).collectLatest { chatRooms ->
                        viewModelStateFlow.update { viewModelState ->
                            viewModelState.copy(
                                chatRooms = chatRooms,
                            )
                        }
                    }
                }

                is Navigator.Project.ProjectType.Project -> {
                    appDatabase.chatRoomDao().getFromProjectInChatRoomId(
                        projectId = navigator.type.projectId.id,
                        isAsc = false,
                    ).collectLatest { chatRooms ->
                        viewModelStateFlow.update { viewModelState ->
                            viewModelState.copy(
                                chatRooms = chatRooms,
                            )
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        systemMessage = BuiltinProjectUiState.SystemMessage(
                            text = viewModelState.systemInfo?.getInfo()?.systemMessage.orEmpty(),
                            editable = true,
                            listener = systemMessageListener,
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
                                },
                            )
                        },
                    )
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            when (navigator.type) {
                is Navigator.Project.ProjectType.Builtin -> {
                    val systemInfo = GetBuiltinProjectInfoUseCase().exec(navigator.type.builtinProjectId)

                    viewModelStateFlow.update {
                        it.copy(
                            systemInfo = ViewModelState.SystemInfoType.BuiltinInfo(systemInfo),
                        )
                    }
                }

                is Navigator.Project.ProjectType.Project -> {
                    appDatabase.projectDao().get(projectId = navigator.type.projectId.id).collectLatest { project ->
                        viewModelStateFlow.update { viewModelState ->
                            viewModelState.copy(
                                systemInfo = ViewModelState.SystemInfoType.Project(project),
                            )
                        }
                    }
                }
            }
        }
    }

    inner class ChatRoomListener(private val chatRoomId: ChatRoomId) : BuiltinProjectUiState.History.Listener {
        override fun onClick() {
            navControllerProvider().navigate(
                Navigator.Chat(
                    openContext = Navigator.Chat.ChatOpenContext.OpenChat(
                        chatRoomId = chatRoomId,
                    ),
                ),
            )
        }
    }

    private data class ViewModelState(
        val uriList: List<String> = listOf(),
        val mediaLoading: Boolean = false,
        val chatRooms: List<ChatRoomWithSummary>? = null,
        val systemInfo: SystemInfoType? = null,
    ) {
        sealed interface SystemInfoType {
            data class BuiltinInfo(val info: GetBuiltinProjectInfoUseCase.Info) : SystemInfoType {
                override fun getInfo(): Info {
                    return Info(
                        systemMessage = info.systemMessage,
                        format = info.format,
                        responseTransformer = info.responseTransformer,
                        model = info.model,
                    )
                }
            }

            data class Project(val project: net.matsudamper.gptclient.room.entity.Project) : SystemInfoType {
                override fun getInfo(): Info {
                    return Info(
                        systemMessage = project.systemMessage,
                        format = ChatGptClient.Format.Text,
                        responseTransformer = { AnnotatedString(it) },
                        model = ChatGptModel.entries.firstOrNull { it.modelName == project.modelName }
                            ?: ChatGptModel.Gpt4oMini,
                    )
                }
            }

            fun getInfo(): Info
        }

        data class Info(
            val systemMessage: String,
            val format: ChatGptClient.Format,
            val responseTransformer: (String) -> AnnotatedString,
            val model: ChatGptModel,
        )
    }
}
