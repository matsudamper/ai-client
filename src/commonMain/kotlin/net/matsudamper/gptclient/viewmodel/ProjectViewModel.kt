package net.matsudamper.gptclient.viewmodel

import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.matsudamper.gptclient.PlatformRequest
import net.matsudamper.gptclient.entity.ChatGptModel
import net.matsudamper.gptclient.gpt.ChatGptClientInterface
import net.matsudamper.gptclient.navigation.Navigator
import net.matsudamper.gptclient.room.AppDatabase
import net.matsudamper.gptclient.room.entity.ChatRoomId
import net.matsudamper.gptclient.room.entity.ChatRoomWithSummary
import net.matsudamper.gptclient.ui.ProjectUiState
import net.matsudamper.gptclient.ui.chat.ChatMessageComposableInterface
import net.matsudamper.gptclient.ui.chat.TextMessageComposableInterface

class ProjectViewModel(
    private val navigator: Navigator.Project,
    private val platformRequest: PlatformRequest,
    private val appDatabase: AppDatabase,
    private val navControllerProvider: () -> NavHostController,
) : ViewModel() {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())
    private val systemMessageListener = object : ProjectUiState.SystemMessage.Listener {
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
    private val listener = object : ProjectUiState.Listener {
        override fun recordVoice() {
        }

        override fun changeName(text: String) {
            when (val info = viewModelStateFlow.value.systemInfo) {
                is ViewModelState.SystemInfoType.BuiltinInfo,
                null,
                -> return

                is ViewModelState.SystemInfoType.Project -> {
                    viewModelScope.launch {
                        appDatabase.projectDao().update(
                            info.project.copy(
                                name = text,
                            ),
                        )
                    }
                }
            }
        }

        override fun delete() {
            when (val systemInfo = viewModelStateFlow.value.systemInfo) {
                is ViewModelState.SystemInfoType.BuiltinInfo,
                null,
                -> return

                is ViewModelState.SystemInfoType.Project -> {
                    viewModelScope.launch {
                        withContext(Dispatchers.IO) {
                            appDatabase.projectDao().delete(systemInfo.project)
                        }
                        navControllerProvider().popBackStack()
                    }
                }
            }
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
                    Navigator.Chat.ChatType.BuiltinProject(
                        builtinProjectId = navigator.type.builtinProjectId,
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
                        model = viewModelStateFlow.value.overwriteModel ?: systemInfo.getInfo().model,
                    ),
                ),
            )

            viewModelStateFlow.update {
                it.copy(uriList = listOf())
            }
        }
    }
    val uiStateFlow: StateFlow<ProjectUiState> = MutableStateFlow(
        ProjectUiState(
            projectName = navigator.title,
            chatRoomsState = ProjectUiState.ChatRoomsState.Loading,
            visibleMediaLoading = false,
            selectedMedia = listOf(),
            systemMessage = ProjectUiState.SystemMessage(
                text = "",
                editable = false,
                listener = systemMessageListener,
            ),
            modelState = createModelState(ChatGptModel.GPT_4O_MINI),
            listener = listener,
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
                    val editable = when (viewModelState.systemInfo) {
                        null,
                        is ViewModelState.SystemInfoType.BuiltinInfo,
                        -> false

                        is ViewModelState.SystemInfoType.Project -> true
                    }
                    uiState.copy(
                        projectName = when (viewModelState.systemInfo) {
                            is ViewModelState.SystemInfoType.Project -> viewModelState.systemInfo.project.name
                            is ViewModelState.SystemInfoType.BuiltinInfo,
                            null,
                            -> navigator.title
                        },
                        systemMessage = ProjectUiState.SystemMessage(
                            text = viewModelState.systemInfo?.getInfo()?.systemMessage.orEmpty(),
                            editable = editable,
                            listener = systemMessageListener,
                        ),
                        selectedMedia = viewModelState.uriList,
                        visibleMediaLoading = viewModelState.mediaLoading,
                        chatRoomsState = run rooms@{
                            val chatRooms = viewModelState.chatRooms
                                ?: return@rooms ProjectUiState.ChatRoomsState.Loading

                            ProjectUiState.ChatRoomsState.Loaded(
                                histories = chatRooms.map { room ->
                                    ProjectUiState.History(
                                        text = room.chatRoom.summary ?: room.textMessage?.replace("\n", "") ?: "空白",
                                        listener = ChatRoomListener(
                                            chatRoomId = room.chatRoom.id,
                                        ),
                                    )
                                },
                            )
                        },
                        modelState = createModelState(
                            viewModelState.overwriteModel
                                ?: viewModelState.systemInfo?.getInfo()?.model
                                ?: ChatGptModel.GPT_4O_MINI,
                        ),
                    )
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            when (navigator.type) {
                is Navigator.Project.ProjectType.Builtin -> {
                    val systemInfo = GetBuiltinProjectInfoUseCase().exec(
                        navigator.type.builtinProjectId,
                        platformRequest = platformRequest,
                    )

                    viewModelStateFlow.update {
                        it.copy(
                            systemInfo = ViewModelState.SystemInfoType.BuiltinInfo(systemInfo),
                        )
                    }
                }

                is Navigator.Project.ProjectType.Project -> {
                    appDatabase.projectDao().get(projectId = navigator.type.projectId.id)
                        .filterNotNull()
                        .collectLatest { project ->
                            println("project -> $project")
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

    inner class ChatRoomListener(private val chatRoomId: ChatRoomId) : ProjectUiState.History.Listener {
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

    private fun createModelState(selectedModel: ChatGptModel): ProjectUiState.ModelState = ProjectUiState.ModelState(
        selectedModel = selectedModel.modelName,
        models = ChatGptModel.entries.map { model ->
            ProjectUiState.ModelState.Item(
                modelName = model.modelName,
                selected = model == selectedModel,
                listener = object : ProjectUiState.ModelState.ItemListener {
                    override fun onClick() {
                        when (val info = viewModelStateFlow.value.systemInfo) {
                            is ViewModelState.SystemInfoType.BuiltinInfo,
                            null,
                            -> {
                                viewModelStateFlow.update {
                                    it.copy(
                                        overwriteModel = model,
                                    )
                                }
                            }

                            is ViewModelState.SystemInfoType.Project -> {
                                viewModelScope.launch {
                                    appDatabase.projectDao().update(
                                        info.project.copy(
                                            modelName = model.modelName,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                },
            )
        },
    )

    private data class ViewModelState(
        val uriList: List<String> = listOf(),
        val mediaLoading: Boolean = false,
        val chatRooms: List<ChatRoomWithSummary>? = null,
        val systemInfo: SystemInfoType? = null,
        val overwriteModel: ChatGptModel? = null,
    ) {
        sealed interface SystemInfoType {
            data class BuiltinInfo(val info: GetBuiltinProjectInfoUseCase.Info) : SystemInfoType {
                override fun getInfo(): Info = Info(
                    systemMessage = info.systemMessage,
                    format = info.format,
                    responseTransformer = info.responseTransformer,
                    model = info.model,
                )
            }

            data class Project(val project: net.matsudamper.gptclient.room.entity.Project) : SystemInfoType {
                override fun getInfo(): Info = Info(
                    systemMessage = project.systemMessage,
                    format = ChatGptClientInterface.Format.Text,
                    responseTransformer = { TextMessageComposableInterface(AnnotatedString(it)) },
                    model = ChatGptModel.entries.firstOrNull { it.modelName == project.modelName }
                        ?: ChatGptModel.GPT_4O_MINI,
                )
            }

            fun getInfo(): Info
        }

        data class Info(val systemMessage: String, val format: ChatGptClientInterface.Format, val responseTransformer: (String) -> ChatMessageComposableInterface, val model: ChatGptModel)
    }
}
