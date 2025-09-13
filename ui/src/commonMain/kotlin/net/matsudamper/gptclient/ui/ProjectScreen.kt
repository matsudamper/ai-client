package net.matsudamper.gptclient.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.MessageSquare
import net.matsudamper.gptclient.ui.component.ChatFooter
import net.matsudamper.gptclient.ui.component.ChatFooterImage

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
    val visibleMediaLoading: Boolean,
    val chatRoomsState: ChatRoomsState,
    val modelState: ModelState,
    val listener: Listener,
) {
    data class ModelState(
        val selectedModel: String,
        val models: List<Item>,
    ) {
        data class Item(
            val modelName: String,
            val selected: Boolean,
            val listener: ItemListener,
        )

        @Immutable
        interface ItemListener {
            fun onClick()
        }
    }

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
    val density = LocalDensity.current
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
    Column(
        modifier = modifier
            .testTag(ProjectScreenTestTag.Root.testTag())
            .imePadding(),
    ) {
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
        val itemHorizontalPadding = 12.dp
        Box(
            modifier = Modifier.fillMaxWidth()
                .weight(1f),
        ) {
            var menuHeight by remember { mutableStateOf(0.dp) }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = menuHeight),
            ) {
                when (uiState.chatRoomsState) {
                    is ProjectUiState.ChatRoomsState.Loaded -> {
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
                                LaunchedEffect(state.text) {
                                    uiState.systemMessage.listener.onChange(state.text.toString())
                                }
                                BasicTextField(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.small)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(8.dp),
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    state = state,
                                    enabled = uiState.systemMessage.editable,
                                )
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
                        items(uiState.chatRoomsState.histories) { history ->
                            Row(
                                modifier = Modifier.fillMaxSize()
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

                    is ProjectUiState.ChatRoomsState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
            ModelMenu(
                uiState = uiState.modelState,
                modifier = Modifier.fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .onGloballyPositioned {
                        menuHeight = with(density) {
                            it.size.height.toDp()
                        }
                    },
            )
        }
        val state = rememberTextFieldState()
        ChatFooter(
            modifier = Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .navigationBarsPadding(),
            textFieldState = state,
            onClickAddImage = { uiState.listener.selectMedia() },
            onClickVoice = { uiState.listener.recordVoice() },
            selectedMedia = uiState.selectedMedia,
            visibleMediaLoading = uiState.visibleMediaLoading,
            onClickRetry = null,
            onClickSend = {
                uiState.listener.send(state.text.toString())
                state.clearText()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelMenu(
    uiState: ProjectUiState.ModelState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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
                            Text(text = model.modelName)
                        },
                        onClick = {
                            expanded = false
                            model.listener.onClick()
                        },
                    )
                }
            }
            OutlinedButton(
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .padding(8.dp),
                onClick = { },
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(uiState.selectedModel)
                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }
        }
    }
}
