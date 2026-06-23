package net.matsudamper.gptclient.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.MessageSquare
import net.matsudamper.gptclient.ui.component.ChatFooter
import net.matsudamper.gptclient.ui.component.ChatFooterImage
import net.matsudamper.gptclient.ui.component.ModelSelectorBar
import net.matsudamper.gptclient.ui.component.ModelSelectorUiState

sealed interface ProjectScreenTestTag {
    object Root : ProjectScreenTestTag

    fun testTag(): String {
        return this::class.qualifiedName!!
    }
}

data class ProjectUiState(
    val projectName: String,
    val selectedMedia: List<ChatFooterImage>,
    val systemMessage: SystemMessage,
    val jsonUi: JsonUi?,
    val visibleMediaLoading: Boolean,
    val enableSend: Boolean,
    val chatRoomsState: ChatRoomsState,
    val modelState: ModelSelectorUiState,
    val listener: Listener,
) {
    @Immutable
    sealed interface ChatRoomsState {
        object Loading : ChatRoomsState
        data class Loaded(
            val histories: List<History>,
        ) : ChatRoomsState
    }

    data class SystemMessage(
        val text: String,
        val editable: Boolean,
        val listener: Listener,
    ) {
        interface Listener {
            fun onChange(text: String)
        }
    }

    data class JsonUi(
        val enabled: Boolean,
        val listener: Listener,
    ) {
        @Immutable
        interface Listener {
            fun onChange(enabled: Boolean)
        }
    }

    data class History(
        val text: String,
        val listener: Listener,
    ) {
        @Immutable
        interface Listener {
            fun onClick()
        }
    }

    @Immutable
    interface Listener {
        fun send(text: String)
        fun selectMedia()
        fun recordVoice()
        fun changeName(text: String)
        fun delete()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectScreen(
    uiState: ProjectUiState,
    onClickMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var visibleDeleteDialog by remember { mutableStateOf(false) }
    if (visibleDeleteDialog) {
        AlertDialog(
            onDismissRequest = { visibleDeleteDialog = false },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        uiState.listener.delete()
                        visibleDeleteDialog = false
                    },
                ) {
                    Text("削除")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { visibleDeleteDialog = false },
                ) {
                    Text("キャンセル")
                }
            },
            title = {
                Text("削除しますか？")
            },
        )
    }
    var visibleChangeNameDialog by remember { mutableStateOf(false) }
    if (visibleChangeNameDialog) {
        var newNameState = rememberTextFieldState(uiState.projectName)
        AlertDialog(
            onDismissRequest = { visibleChangeNameDialog = false },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        uiState.listener.changeName(newNameState.text.toString())
                        visibleChangeNameDialog = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { visibleChangeNameDialog = false },
                ) {
                    Text("キャンセル")
                }
            },
            title = {
                Text("名前を変更しますか？")
            },
            text = {
                BasicTextField(
                    modifier = Modifier.fillMaxWidth(),
                    state = newNameState,
                    decorator = {
                        TextFieldDefaults.DecorationBox(
                            value = newNameState.text.toString(),
                            innerTextField = {
                                it()
                            },
                            enabled = true,
                            singleLine = true,
                            visualTransformation = VisualTransformation.None,
                            interactionSource = remember { MutableInteractionSource() },
                        )
                    },
                )
            },
        )
    }
    Scaffold(
        modifier = modifier
            .testTag(ProjectScreenTestTag.Root.testTag())
            .imePadding(),
        topBar = {
            TopAppBar(
                modifier = Modifier,
                navigationIcon = {
                    IconButton(onClick = { onClickMenu() }) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = null)
                    }
                },
                title = {
                    Text(text = uiState.projectName)
                },
                actions = {
                    var visibleMenu by remember { mutableStateOf(false) }
                    if (visibleMenu) {
                        DropdownMenu(
                            expanded = true,
                            onDismissRequest = { visibleMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text("名前変更")
                                },
                                onClick = { visibleChangeNameDialog = true },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text("削除")
                                },
                                onClick = { visibleDeleteDialog = true },
                            )
                        }
                    }

                    IconButton(onClick = { visibleMenu = !visibleMenu }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(innerPadding),
        ) {
            val itemHorizontalPadding = 12.dp
            when (val chatRoomsState = uiState.chatRoomsState) {
                is ProjectUiState.ChatRoomsState.Loaded -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                            .weight(1f),
                    ) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = itemHorizontalPadding),
                            ) {
                                Text(
                                    style = MaterialTheme.typography.titleLarge,
                                    text = "命令",
                                )
                                val state = rememberTextFieldState(uiState.systemMessage.text)
                                val collapsedMaxHeight = 160.dp
                                var expandedSystemMessage by remember { mutableStateOf(false) }
                                LaunchedEffect(state.text) {
                                    uiState.systemMessage.listener.onChange(state.text.toString())
                                }
                                val canExpandSystemMessage = state.text.length > 200
                                val systemMessageModifier = if (expandedSystemMessage) {
                                    Modifier.fillMaxWidth()
                                } else {
                                    Modifier.fillMaxWidth().heightIn(max = collapsedMaxHeight)
                                }
                                BasicTextField(
                                    modifier = systemMessageModifier
                                        .clip(MaterialTheme.shapes.small)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(8.dp),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                    state = state,
                                    enabled = uiState.systemMessage.editable,
                                )
                                if (canExpandSystemMessage && !expandedSystemMessage) {
                                    OutlinedButton(
                                        onClick = { expandedSystemMessage = true },
                                    ) {
                                        Text("展開")
                                    }
                                }
                                if (uiState.jsonUi != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(top = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "JSONでUIを組み立てる",
                                                style = MaterialTheme.typography.bodyLarge,
                                            )
                                            Text(
                                                text = "応答をJSONで受け取り、UIとして表示します。形式の指示は自動で追加されます。",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Switch(
                                            checked = uiState.jsonUi.enabled,
                                            onCheckedChange = { uiState.jsonUi.listener.onChange(it) },
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                        item {
                            Text(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = itemHorizontalPadding),
                                style = MaterialTheme.typography.titleLarge,
                                text = "履歴",
                            )
                        }
                        items(chatRoomsState.histories) { history ->
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable {
                                        history.listener.onClick()
                                    }
                                    .padding(
                                        horizontal = itemHorizontalPadding,
                                        vertical = 12.dp,
                                    ),
                            ) {
                                Icon(
                                    imageVector = FeatherIcons.MessageSquare,
                                    contentDescription = null,
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    history.text,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }

                is ProjectUiState.ChatRoomsState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            val state = rememberTextFieldState()
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .navigationBarsPadding(),
            ) {
                ModelSelectorBar(
                    uiState = uiState.modelState,
                )
                ChatFooter(
                    modifier = Modifier.fillMaxWidth(),
                    textFieldState = state,
                    onClickAddImage = { uiState.listener.selectMedia() },
                    onClickVoice = { uiState.listener.recordVoice() },
                    selectedMedia = uiState.selectedMedia,
                    visibleMediaLoading = uiState.visibleMediaLoading,
                    enableSend = uiState.enableSend,
                    onClickRetry = null,
                    onClickSend = {
                        uiState.listener.send(state.text.toString())
                        state.clearText()
                    },
                )
            }
        }
    }
}
