package net.matsudamper.gptclient.viewmodel

import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.matsudamper.gptclient.PlatformRequest
import net.matsudamper.gptclient.client.AiClient
import net.matsudamper.gptclient.datastore.SettingDataStore
import net.matsudamper.gptclient.entity.ChatGptModel
import net.matsudamper.gptclient.localmodel.LocalModelDefinition
import net.matsudamper.gptclient.localmodel.LocalModelId
import net.matsudamper.gptclient.localmodel.LocalModelRepository
import net.matsudamper.gptclient.navigation.AppNavigator
import net.matsudamper.gptclient.navigation.Navigator
import net.matsudamper.gptclient.room.AppDatabase
import net.matsudamper.gptclient.room.entity.ChatRoomId
import net.matsudamper.gptclient.room.entity.ChatRoomWithSummary
import net.matsudamper.gptclient.ui.ProjectUiState
import net.matsudamper.gptclient.ui.chat.ChatMessageComposableInterface
import net.matsudamper.gptclient.ui.chat.TextMessageComposableInterface
import net.matsudamper.gptclient.ui.component.ChatFooterImage
import net.matsudamper.gptclient.util.EventSender

class ProjectViewModel(
    private val navigator: Navigator.Project,
    private val appDatabase: AppDatabase,
    private val appNavigator: AppNavigator,
    private val settingDataStore: SettingDataStore,
    private val localModelRepository: LocalModelRepository,
) : ViewModel() {
    private val eventSender = EventSender<Event>()
    val eventHandler = eventSender.asHandler()

    interface Event {
        fun providePlatformRequest(): PlatformRequest
    }

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
                        appNavigator.popBackStack()
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
                    val uriList = withPlatformRequest {
                        getMediaList()
                    }
                    viewModelStateFlow.update {
                        it.copy(
                            uriList = uriList.map { imageUrl ->
                                ChatFooterImage(
                                    imageUri = imageUrl,
                                    rect = null,
                                    listener = ChatFooterImageListener(imageUrl),
                                )
                            },
                        )
                    }
                } finally {
                    viewModelStateFlow.update {
                        it.copy(mediaLoading = false)
                    }
                }
            }
        }

        override fun send(text: String) {
            viewModelStateFlow.update {
                it.copy(
                    isLoading = true,
                )
            }
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
            viewModelScope.launch {
                appNavigator.navigate(
                    Navigator.Chat(
                        openContext = Navigator.Chat.ChatOpenContext.NewMessage(
                            initialMessage = text,
                            uriList = viewModelStateFlow.value.uriList.mapNotNull map@{
                                val rect = it.rect ?: return@map it.imageUri

                                withPlatformRequest {
                                    cropImage(
                                        it.imageUri,
                                        PlatformRequest.CropRect(
                                            left = rect.left,
                                            top = rect.top,
                                            right = rect.right,
                                            bottom = rect.bottom,
                                        ),
                                    )
                                }
                            },
                            chatType = chatType,
                            model = viewModelStateFlow.value.overwriteModel ?: systemInfo.getInfo().model,
                        ),
                    ),
                )

                viewModelStateFlow.update {
                    it.copy(
                        uriList = listOf(),
                        isLoading = false,
                    )
                }
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
            modelState = createModelState(ChatGptModel.Remote.Gpt.Gpt5Nano),
            enableSend = false,
            listener = listener,
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            settingDataStore.getActiveLocalModelKeysFlow().collectLatest { activeKeys ->
                viewModelStateFlow.update { it.copy(activeLocalModelKeys = activeKeys) }
            }
        }
        viewModelScope.launch {
            val defs = localModelRepository.getModels()
            viewModelStateFlow.update { it.copy(localModelDefs = defs) }
        }
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
                        enableSend = !viewModelState.mediaLoading,
                        chatRoomsState = run rooms@{
                            val chatRooms = viewModelState.chatRooms
                                ?: return@rooms ProjectUiState.ChatRoomsState.Loading
                            if (viewModelState.isLoading) {
                                return@rooms ProjectUiState.ChatRoomsState.Loading
                            }

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
                                ?: ChatGptModel.Remote.Gpt.Gpt5Nano,
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
                    viewModelStateFlow.update {
                        it.copy(
                            systemInfo = ViewModelState.SystemInfoType.BuiltinInfo(
                                createBuiltinProjectInfo(navigator.type.builtinProjectId),
                            ),
                        )
                    }
                }

                is Navigator.Project.ProjectType.Project -> {
                    appDatabase.projectDao().get(projectId = navigator.type.projectId.id)
                        .filterNotNull()
                        .collectLatest { project ->
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

    private fun createBuiltinProjectInfo(
        builtinProjectId: net.matsudamper.gptclient.room.entity.BuiltinProjectId,
    ): GetBuiltinProjectInfoUseCase.Info {
        return GetBuiltinProjectInfoUseCase().exec(
            builtinProjectId = builtinProjectId,
            onCopyEmoji = { emoji ->
                launchWithPlatformRequest {
                    copyToClipboard(emoji)
                }
            },
        )
    }

    private suspend fun <R> withPlatformRequest(
        block: suspend PlatformRequest.() -> R,
    ): R {
        return eventSender.send { event ->
            event.providePlatformRequest().block()
        }
    }

    private fun launchWithPlatformRequest(
        block: suspend PlatformRequest.() -> Unit,
    ) {
        viewModelScope.launch {
            withPlatformRequest(block)
        }
    }

    inner class ChatRoomListener(private val chatRoomId: ChatRoomId) : ProjectUiState.History.Listener {
        override fun onClick() {
            appNavigator.navigate(
                Navigator.Chat(
                    openContext = Navigator.Chat.ChatOpenContext.OpenChat(
                        chatRoomId = chatRoomId,
                    ),
                ),
            )
        }
    }

    private inner class ChatFooterImageListener(private val imageUrl: String) : ChatFooterImage.Listener {
        override fun crop(rect: ChatFooterImage.Rect) {
            viewModelStateFlow.update {
                it.copy(
                    uriList = it.uriList.map { image ->
                        if (image.imageUri == imageUrl) {
                            image.copy(
                                rect = rect,
                            )
                        } else {
                            image
                        }
                    },
                )
            }
        }

        override fun delete() {
            viewModelStateFlow.update {
                it.copy(
                    uriList = it.uriList.filter { image -> image.imageUri != imageUrl },
                )
            }
        }
    }

    private fun createModelState(selectedModel: ChatGptModel): ProjectUiState.ModelState {
        val localDefs = viewModelStateFlow.value.localModelDefs
        val allModels = ChatGptModel.entries + viewModelStateFlow.value.activeLocalModelKeys.mapNotNull { key ->
            val def = localDefs.find { it.modelId == key } ?: return@mapNotNull null
            ChatGptModel.Local(
                modelKey = def.modelId.value,
                displayName = def.displayName,
                enableImage = def.enableImage,
                defaultToken = def.defaultToken,
            )
        }
        return ProjectUiState.ModelState(
        selectedModel = selectedModel.displayName,
        models = allModels.map { model ->
            ProjectUiState.ModelState.Item(
                modelName = model.displayName,
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
                                            modelName = model.modelKey,
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
    }

    private data class ViewModelState(
        val uriList: List<ChatFooterImage> = listOf(),
        val mediaLoading: Boolean = false,
        val chatRooms: List<ChatRoomWithSummary>? = null,
        val systemInfo: SystemInfoType? = null,
        val overwriteModel: ChatGptModel? = null,
        val activeLocalModelKeys: Set<LocalModelId> = emptySet(),
        val localModelDefs: List<LocalModelDefinition> = emptyList(),
        val isLoading: Boolean = false,
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
                    format = AiClient.Format.Text,
                    responseTransformer = { TextMessageComposableInterface(AnnotatedString(it)) },
                    model = ChatGptModel.entries.firstOrNull { it.modelKey == project.modelName }
                        ?: ChatGptModel.Remote.Gpt.Gpt5Nano,
                )
            }

            fun getInfo(): Info
        }

        data class Info(val systemMessage: String, val format: AiClient.Format, val responseTransformer: (String) -> ChatMessageComposableInterface, val model: ChatGptModel)
    }
}
