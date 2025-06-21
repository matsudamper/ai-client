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
            listener = object : NewChatUiState.Listener {
                override fun send(text: String) {
                    navControllerProvider().navigate(
                        Navigator.Chat(
                            Navigator.Chat.ChatOpenContext.NewMessage(
                                initialMessage = text,
                                uriList = viewModelStateFlow.value.mediaList,
                                chatType = Navigator.Chat.ChatType.Normal,
                                model = viewModelStateFlow.value.selectedModel,
                            ),
                        ),
                    )
                }

                override fun onClickSelectMedia() {
                    viewModelScope.launch {
                        try {
                            viewModelStateFlow.update {
                                it.copy(mediaLoading = true)
                            }
                            val media = platformRequest.getMedia()
                            viewModelStateFlow.update {
                                it.copy(mediaList = media)
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
                        projects = builtinProjects.plus(
                            viewModelState.projects.orEmpty().map {
                                NewChatUiState.Project(
                                    name = it.name,
                                    icon = NewChatUiState.Project.Icon.Favorite,
                                    listener = object : NewChatUiState.Project.Listener {
                                        override fun onClick() {
                                            navControllerProvider().navigate(
                                                Navigator.Project(
                                                    title = it.name,
                                                    type = Navigator.Project.ProjectType.Project(
                                                        projectId = it.id,
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

    private data class ViewModelState(
        val mediaList: List<String> = listOf(),
        val mediaLoading: Boolean = false,
        val selectedModel: ChatGptModel = ChatGptModel.Gpt4oMini,
        val projectNameDialog: NewChatUiState.ProjectNameDialog? = null,
        val projects: List<Project>? = null,
    )
}
