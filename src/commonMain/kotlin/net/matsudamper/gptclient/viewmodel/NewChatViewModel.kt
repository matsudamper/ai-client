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
import net.matsudamper.gptclient.entity.ChatGptModel
import net.matsudamper.gptclient.entity.Emoji
import net.matsudamper.gptclient.entity.Money
import net.matsudamper.gptclient.navigation.Navigator
import net.matsudamper.gptclient.room.AppDatabase
import net.matsudamper.gptclient.room.entity.BuiltinProjectId
import net.matsudamper.gptclient.room.entity.Project
import net.matsudamper.gptclient.room.entity.ProjectId
import net.matsudamper.gptclient.ui.NewChatUiState
import net.matsudamper.gptclient.ui.component.ChatFooterImage

class NewChatViewModel(private val platformRequest: PlatformRequest, private val appDatabase: AppDatabase, navControllerProvider: () -> NavHostController) : ViewModel() {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())
    private val builtinProjects = listOf(
        NewChatUiState.Project(
            name = "カレンダー追加",
            icon = NewChatUiState.Project.Icon.Calendar,
            listener = object : NewChatUiState.Project.Listener {
                override fun onClick() {
                    navControllerProvider().navigate(
                        Navigator.Project(
                            title = "カレンダー追加",
                            type = Navigator.Project.ProjectType.Builtin(
                                BuiltinProjectId.Calendar,
                            ),
                        ),
                    )
                }
            },
        ),
        NewChatUiState.Project(
            name = "家計簿追加",
            icon = NewChatUiState.Project.Icon.Card,
            listener = object : NewChatUiState.Project.Listener {
                override fun onClick() {
                    navControllerProvider().navigate(
                        Navigator.Project(
                            title = "家計簿追加",
                            type = Navigator.Project.ProjectType.Builtin(
                                BuiltinProjectId.Money,
                            ),
                        ),
                    )
                }
            },
        ),
        NewChatUiState.Project(
            name = "絵文字",
            icon = NewChatUiState.Project.Icon.Emoji,
            listener = object : NewChatUiState.Project.Listener {
                override fun onClick() {
                    navControllerProvider().navigate(
                        Navigator.Project(
                            title = "絵文字",
                            type = Navigator.Project.ProjectType.Builtin(
                                BuiltinProjectId.Emoji,
                            ),
                        ),
                    )
                }
            },
        ),
    )
    public val uiState: StateFlow<NewChatUiState> = MutableStateFlow(
        NewChatUiState(
            projects = builtinProjects,
            selectedMedia = listOf(),
            visibleMediaLoading = false,
            models = ChatGptModel.entries.map { gptModel ->
                NewChatUiState.Model(
                    name = gptModel.modelName,
                    listener = object : NewChatUiState.Model.Listener {
                        override fun onClick() {
                            viewModelStateFlow.update { viewModelState ->
                                viewModelState.copy(selectedModel = gptModel)
                            }
                        }
                    },
                )
            },
            selectedModel = "",
            projectNameDialog = null,
            isLoading = false,
            enableSend = false,
            listener = object : NewChatUiState.Listener {
                override fun send(text: String) {
                    viewModelScope.launch {
                        viewModelStateFlow.update {
                            it.copy(
                                isLoading = true,
                            )
                        }
                        navControllerProvider().navigate(
                            Navigator.Chat(
                                Navigator.Chat.ChatOpenContext.NewMessage(
                                    initialMessage = text,
                                    uriList = viewModelStateFlow.value.mediaList.mapNotNull map@{
                                        val rect = it.rect ?: return@map it.imageUri
                                        platformRequest.cropImage(
                                            uri = it.imageUri,
                                            cropRect = PlatformRequest.CropRect(
                                                left = rect.left,
                                                top = rect.top,
                                                right = rect.right,
                                                bottom = rect.bottom,
                                            ),
                                        )
                                    },
                                    chatType = Navigator.Chat.ChatType.Normal,
                                    model = viewModelStateFlow.value.selectedModel,
                                ),
                            ),
                        )
                        viewModelStateFlow.update {
                            it.copy(
                                isLoading = false,
                            )
                        }
                    }
                }

                override fun onClickSelectMedia() {
                    viewModelScope.launch {
                        try {
                            viewModelStateFlow.update {
                                it.copy(mediaLoading = true)
                            }
                            val imageUrlList = platformRequest.getMediaList()
                            viewModelStateFlow.update {
                                it.copy(
                                    mediaList = imageUrlList.map { imageUrl ->
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

                override fun addProject() {
                    viewModelStateFlow.update {
                        it.copy(
                            projectNameDialog = NewChatUiState.ProjectNameDialog(
                                object : NewChatUiState.ProjectNameDialog.Listener {
                                    override fun onCancel() {
                                        viewModelStateFlow.update { it.copy(projectNameDialog = null) }
                                    }

                                    override fun onDone(text: String) {
                                        viewModelScope.launch {
                                            val projectId = appDatabase.projectDao().insert(
                                                Project(
                                                    index = 0,
                                                    name = text,
                                                    modelName = "",
                                                    systemMessage = "",
                                                ),
                                            )
                                            viewModelStateFlow.update { it.copy(projectNameDialog = null) }
                                            navControllerProvider().navigate(
                                                Navigator.Project(
                                                    title = text,
                                                    type = Navigator.Project.ProjectType.Project(
                                                        projectId = ProjectId(projectId),
                                                    ),
                                                ),
                                            )
                                        }
                                    }
                                },
                            ),
                        )
                    }
                }

                override fun onClickVoice() {
                }
            },
        ),
    ).also { uiState ->
        viewModelScope.launch {
            appDatabase.projectDao().getAll().collectLatest { projects ->
                viewModelStateFlow.update {
                    it.copy(projects = projects)
                }
            }
        }
        viewModelScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                uiState.update {
                    it.copy(
                        selectedMedia = viewModelState.mediaList,
                        visibleMediaLoading = viewModelState.mediaLoading,
                        selectedModel = viewModelState.selectedModel.modelName,
                        projectNameDialog = viewModelState.projectNameDialog,
                        isLoading = viewModelState.isLoading,
                        enableSend = !viewModelState.mediaLoading,
                        projects = builtinProjects.plus(
                            viewModelState.projects.orEmpty().map { project ->
                                NewChatUiState.Project(
                                    name = project.name,
                                    icon = NewChatUiState.Project.Icon.Favorite,
                                    listener = object : NewChatUiState.Project.Listener {
                                        override fun onClick() {
                                            navControllerProvider().navigate(
                                                Navigator.Project(
                                                    title = project.name,
                                                    type = Navigator.Project.ProjectType.Project(
                                                        projectId = project.id,
                                                    ),
                                                ),
                                            )
                                        }
                                    },
                                )
                            },
                        ),
                    )
                }
            }
        }
    }

    private inner class ChatFooterImageListener(private val imageUrl: String) : ChatFooterImage.Listener {
        override fun crop(rect: ChatFooterImage.Rect) {
            viewModelStateFlow.update { viewModelState ->
                val newList = viewModelState.mediaList.map { viewModelStateImage ->
                    if (viewModelStateImage.imageUri == imageUrl) {
                        viewModelStateImage.copy(
                            rect = rect,
                        )
                    } else {
                        viewModelStateImage
                    }
                }
                viewModelState.copy(
                    mediaList = newList,
                )
            }
        }
    }

    private data class ViewModelState(
        val mediaList: List<ChatFooterImage> = listOf(),
        val mediaLoading: Boolean = false,
        val selectedModel: ChatGptModel = ChatGptModel.Gpt5Nano,
        val projectNameDialog: NewChatUiState.ProjectNameDialog? = null,
        val projects: List<Project>? = null,
        val isLoading: Boolean = false,
    )
}
