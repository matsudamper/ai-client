package net.matsudamper.gptclient.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import net.matsudamper.gptclient.ui.chat.ChatMessageComposableInterface
import net.matsudamper.gptclient.ui.component.ChatFooter

data class ChatListUiState(
    val items: List<Message>,
    val title: String,
    val selectedMedia: List<String>,
    val visibleMediaLoading: Boolean,
    val errorDialogMessage: String?,
    val modelLoadingState: ModelLoadingState,
    val listener: Listener,
) {
    sealed interface ModelLoadingState {
        object Loading : ModelLoadingState
        data class Loaded(val models: List<Model>) : ModelLoadingState
    }

    data class Model(
        val modelName: String,
        val selected: Boolean,
        val listener: Listener,
    ) {
        @Immutable
        interface Listener {
            fun onClick()
        }
    }

    sealed interface Message {
        val uiSet: ChatMessageComposableInterface

        data class Agent(override val uiSet: ChatMessageComposableInterface) : Message
        data class User(override val uiSet: ChatMessageComposableInterface) : Message
    }

    @Immutable
    interface Listener {
        fun onClickImage()
        fun onClickVoice()
        fun onClickSend(text: String)
        fun onImageCrop(imageUri: String, cropRect: androidx.compose.ui.geometry.Rect, imageSize: androidx.compose.ui.unit.IntSize) {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun ChatList(
    uiState: ChatListUiState,
    onClickMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.errorDialogMessage != null) {
        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
            ),
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors().copy(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Text(uiState.errorDialogMessage)
            }
        }
    }
    Column(
        modifier = modifier
            .navigationBarsPadding()
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
                Text(text = uiState.title)
            },
            actions = {
                when (uiState.modelLoadingState) {
                    is ChatListUiState.ModelLoadingState.Loading -> Unit
                    is ChatListUiState.ModelLoadingState.Loaded -> {
                        var visibleMenu by remember { mutableStateOf(false) }
                        if (visibleMenu) {
                            DropdownMenu(
                                expanded = true,
                                onDismissRequest = { visibleMenu = false },
                            ) {
                                for (model in uiState.modelLoadingState.models) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(model.modelName)
                                        },
                                        onClick = { model.listener.onClick() },
                                        trailingIcon = {
                                            if (model.selected) {
                                                Icon(imageVector = Icons.Default.Check, contentDescription = "check")
                                            }
                                        },
                                    )
                                }
                            }
                        }

                        IconButton(onClick = { visibleMenu = !visibleMenu }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)
                        }
                    }
                }
            },
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth()
                .weight(1f),
        ) {
            items(uiState.items) { item ->
                Box(modifier = Modifier.padding(vertical = 4.dp)) {
                    when (item) {
                        is ChatListUiState.Message.Agent -> {
                            AgentItem(
                                modifier = Modifier.fillMaxWidth(),
                                item = item,
                            )
                        }

                        is ChatListUiState.Message.User -> {
                            UserItem(
                                modifier = Modifier.fillMaxWidth(),
                                item = item,
                            )
                        }
                    }
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .navigationBarsPadding(),
        ) {
            val state = rememberTextFieldState()
            ChatFooter(
                modifier = Modifier.fillMaxWidth(),
                textFieldState = state,
                onClickImage = { uiState.listener.onClickImage() },
                onClickVoice = { uiState.listener.onClickVoice() },
                selectedMedia = uiState.selectedMedia,
                visibleMediaLoading = uiState.visibleMediaLoading,
                onClickSend = {
                    uiState.listener.onClickSend(state.text.toString())
                    state.clearText()
                },
                onImageCrop = { imageUri, cropRect, imageSize ->
                    uiState.listener.onImageCrop(imageUri, cropRect, imageSize)
                },
            )
        }
    }
}

private val AgentUserHorizontalPadding = 24.dp
private val ChatHorizontalPadding = 12.dp

@Composable
private fun AgentItem(
    item: ChatListUiState.Message.Agent,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            item.uiSet.Content(modifier = Modifier.padding(horizontal = ChatHorizontalPadding))
        }
        Spacer(modifier = Modifier.width(AgentUserHorizontalPadding))
    }
}

@Composable
private fun UserItem(
    item: ChatListUiState.Message.User,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        Spacer(modifier = Modifier.width(AgentUserHorizontalPadding))
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd,
        ) {
            item.uiSet.Content(Modifier.padding(horizontal = ChatHorizontalPadding))
        }
    }
}
