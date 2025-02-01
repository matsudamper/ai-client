package net.matsudamper.gptclient.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import net.matsudamper.gptclient.ui.component.ChatFooter

data class ChatListUiState(
    val items: List<Message>,
    val selectedMedia: List<String>,
    val visibleMediaLoading: Boolean,
    val errorDialogMessage: String?,
    val listener: Listener,
) {
    sealed interface Message {
        val content: MessageContent

        data class Agent(override val content: MessageContent) : Message
        data class User(override val content: MessageContent) : Message
    }

    sealed interface MessageContent {
        data class Text(val message: AnnotatedString) : MessageContent
        data class Image(val url: String) : MessageContent
        data object Loading : MessageContent
    }

    @Immutable
    interface Listener {
        fun onClickImage()
        fun onClickVoice()
        fun onClickSend(text: String)
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
            )
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors().copy(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            ) {
                Text(uiState.errorDialogMessage)
            }
        }
    }
    var showImageUri by remember { mutableStateOf<String?>(null) }
    if (showImageUri != null) {
        Dialog(
            onDismissRequest = { showImageUri = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
            )
        ) {
            AsyncImage(
                modifier = Modifier.fillMaxSize()
                    .zoomable(rememberZoomState()),
                model = showImageUri.orEmpty(),
                contentScale = ContentScale.Fit,
                contentDescription = null,
            )
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

            }
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
                                onClickImage = { showImageUri = it }
                            )
                        }

                        is ChatListUiState.Message.User -> {
                            UserItem(
                                modifier = Modifier.fillMaxWidth(),
                                item = item,
                                onClickImage = { showImageUri = it }
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
            )
        }
    }
}

private val AgentUserHorizontalPadding = 24.dp
private val ChatHorizontalPadding = 12.dp

@Composable
private fun AgentItem(
    item: ChatListUiState.Message.Agent,
    onClickImage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            when (val content = item.content) {
                is ChatListUiState.MessageContent.Text -> {
                    TextContentItem(
                        modifier = Modifier.padding(horizontal = ChatHorizontalPadding),
                        item = content,
                    )
                }

                is ChatListUiState.MessageContent.Image -> {
                    ImageContentItem(
                        modifier = Modifier.padding(horizontal = ChatHorizontalPadding),
                        item = content,
                        onClickImage = onClickImage,
                    )
                }

                ChatListUiState.MessageContent.Loading -> {
                    LoadingItem(
                        modifier = Modifier.padding(horizontal = ChatHorizontalPadding),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(AgentUserHorizontalPadding))
    }
}

@Composable
private fun UserItem(
    item: ChatListUiState.Message.User,
    onClickImage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        Spacer(modifier = Modifier.width(AgentUserHorizontalPadding))
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd,
        ) {
            when (val content = item.content) {
                is ChatListUiState.MessageContent.Text -> {
                    TextContentItem(
                        modifier = Modifier.padding(horizontal = ChatHorizontalPadding),
                        item = content,
                    )
                }

                is ChatListUiState.MessageContent.Image -> {
                    ImageContentItem(
                        item = content,
                        modifier = Modifier.padding(horizontal = ChatHorizontalPadding),
                        onClickImage = onClickImage,
                    )
                }

                ChatListUiState.MessageContent.Loading -> {
                    LoadingItem(
                        modifier = Modifier.padding(horizontal = ChatHorizontalPadding),
                    )
                }
            }
        }
    }
}

@Composable
private fun TextContentItem(
    item: ChatListUiState.MessageContent.Text,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(6.dp),
        text = item.message,
    )
}

@Composable
private fun ImageContentItem(
    item: ChatListUiState.MessageContent.Image,
    modifier: Modifier = Modifier,
    onClickImage: (String) -> Unit,
) {
    AsyncImage(
        modifier = modifier
            .size(200.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClickImage(item.url) },
        contentScale = ContentScale.Crop,
        contentDescription = null,
        model = item.url,
    )
}

@Composable
private fun LoadingItem(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "       "
        )
        LinearProgressIndicator()
    }
}
