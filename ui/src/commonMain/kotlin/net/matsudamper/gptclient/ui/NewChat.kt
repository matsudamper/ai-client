package net.matsudamper.gptclient.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import compose.icons.FeatherIcons
import compose.icons.feathericons.Calendar
import compose.icons.feathericons.CreditCard
import compose.icons.feathericons.Menu
import net.matsudamper.gptclient.ui.component.ChatFooter
import net.matsudamper.gptclient.ui.component.ChatFooterImage

sealed interface NewChatTestTag {
    object Root : NewChatTestTag
    object AddProjectButton : NewChatTestTag

    data class ProjectButton(val index: Int) : NewChatTestTag {
        override fun testTag(): String {
            return super.testTag() + "$$index"
        }
    }

    fun testTag(): String {
        return this::class.qualifiedName!!
    }
}

public data class NewChatUiState(
    val projects: List<Project>,
    val selectedMedia: List<ChatFooterImage>,
    val visibleMediaLoading: Boolean,
    val enableSend: Boolean,
    val models: List<Model>,
    val selectedModel: String,
    val projectNameDialog: ProjectNameDialog?,
    val isLoading: Boolean,
    val listener: Listener,
) {
    data class ProjectNameDialog(
        val listener: Listener,
    ) {
        interface Listener {
            fun onDone(text: String)
            fun onCancel()
        }
    }

    data class Model(
        val name: String,
        val listener: Listener,
    ) {
        interface Listener {
            fun onClick()
        }
    }

    data class Project(
        val name: String,
        val icon: Icon,
        val color: Color? = null,
        val listener: Listener,
    ) {
        enum class Icon {
            Calendar,
            Card,
            Emoji,
            Favorite,
        }

        @Immutable
        interface Listener {
            fun onClick()
        }
    }

    @Immutable
    interface Listener {
        fun send(text: String)
        fun onClickSelectMedia()
        fun onClickVoice()
        fun addProject()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun NewChat(
    uiState: NewChatUiState,
    onClickMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    run {
        val projectNameDialog = uiState.projectNameDialog
        if (projectNameDialog != null) {
            val state = rememberTextFieldState()
            AlertDialog(
                onDismissRequest = { projectNameDialog.listener.onCancel() },
                dismissButton = {
                    TextButton(onClick = { projectNameDialog.listener.onCancel() }) {
                        Text("キャンセル")
                    }
                },
                text = {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicTextField(
                            modifier = Modifier.fillMaxWidth()
                                .padding(8.dp),
                            state = state,
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            projectNameDialog.listener.onDone(state.text.toString())
                        },
                    ) {
                        Text("決定")
                    }
                },
            )
        }
    }
    Scaffold(
        modifier = modifier.testTag(NewChatTestTag.Root.testTag())
            .imePadding(),
        topBar = {
            TopAppBar(
                modifier = Modifier,
                title = {
                    Text("Chat")
                },
                navigationIcon = {
                    IconButton(onClick = { onClickMenu() }) {
                        Icon(
                            imageVector = FeatherIcons.Menu,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
                .padding(innerPadding),
        ) {
            val maxWidth = this.maxWidth
            Column {
                val projectModifier = Modifier.fillMaxWidth()
                    .height(150.dp)
                    .padding(bottom = 8.dp)

                LazyVerticalGrid(
                    modifier = Modifier.fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        horizontal = 24.dp,
                    ),
                    columns = GridCells.Fixed(
                        ceil(
                            with(LocalDensity.current) {
                                maxWidth.roundToPx() / 160.dp.roundToPx()
                            }.toFloat(),
                        ).toInt(),
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    item(
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        Column {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                text = "Projects",
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    itemsIndexed(uiState.projects) { index, project ->
                        ProjectScreen(
                            modifier = projectModifier.testTag(
                                NewChatTestTag.ProjectButton(index).testTag(),
                            ),
                            content = {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    if (project.color != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .background(
                                                    color = project.color,
                                                    shape = CircleShape,
                                                ),
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                    when (project.icon) {
                                        NewChatUiState.Project.Icon.Calendar -> {
                                            Icon(
                                                imageVector = FeatherIcons.Calendar,
                                                contentDescription = null,
                                            )
                                        }

                                        NewChatUiState.Project.Icon.Card -> {
                                            Icon(
                                                imageVector = FeatherIcons.CreditCard,
                                                contentDescription = null,
                                            )
                                        }

                                        NewChatUiState.Project.Icon.Favorite -> {
                                            Icon(
                                                imageVector = Icons.Filled.Favorite,
                                                contentDescription = null,
                                            )
                                        }

                                        NewChatUiState.Project.Icon.Emoji -> {
                                            Text(
                                                text = "☺️",
                                            )
                                        }
                                    }
                                    Text(project.name)
                                }
                            },
                            onClick = { project.listener.onClick() },
                        )
                    }
                    item {
                        ProjectScreen(
                            modifier = projectModifier.testTag(NewChatTestTag.AddProjectButton.testTag()),
                            content = {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                    )
                                    Text("追加")
                                }
                            },
                            onClick = { uiState.listener.addProject() },
                        )
                    }
                }
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally),
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        for (model in uiState.models) {
                            DropdownMenuItem(
                                text = {
                                    Text(text = model.name)
                                },
                                onClick = {
                                    expanded = false
                                    model.listener.onClick()
                                },
                            )
                        }
                    }
                    OutlinedButton(
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        onClick = { },
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(uiState.selectedModel)
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                val state = rememberTextFieldState()
                ChatFooter(
                    textFieldState = state,
                    onClickAddImage = { uiState.listener.onClickSelectMedia() },
                    onClickVoice = { uiState.listener.onClickVoice() },
                    onClickSend = {
                        uiState.listener.send(state.text.toString())
                        state.clearText()
                    },
                    selectedMedia = uiState.selectedMedia,
                    visibleMediaLoading = uiState.visibleMediaLoading,
                    onClickRetry = null,
                    enableSend = uiState.enableSend || state.text.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .navigationBarsPadding(),
                )
            }
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun ProjectScreen(
    content: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = { onClick() },
    ) {
        content()
    }
}
