package net.matsudamper.gptclient.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import net.matsudamper.gptclient.ui.component.ChatFooter

data class ChatListUiState(
    val items: List<Message>,
    val selectedMedia: List<String>,
    val visibleMediaLoading: Boolean,
    val listener: Listener,
) {
    sealed interface Message {
        val content: MessageContent

        data class Agent(override val content: MessageContent) : Message
        data class User(override val content: MessageContent) : Message
    }

    sealed interface MessageContent {
        data class Text(val message: String) : MessageContent
        data class Image(val url: String) : MessageContent
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
            val imageModifier = Modifier.size(120.dp)
                .padding(12.dp)
            LazyRow {
                items(uiState.selectedMedia) { media ->
                    AsyncImage(
                        modifier = imageModifier,
                        model = media,
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                    )
                }
                if (uiState.visibleMediaLoading) {
                    item {
                        Box(
                            modifier = imageModifier
                                .background(MaterialTheme.colorScheme.secondary),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    }
                }
            }

            val state = rememberTextFieldState()
            ChatFooter(
                modifier = Modifier.fillMaxWidth(),
                state = state,
                onClickImage = { uiState.listener.onClickImage() },
                onClickVoice = { uiState.listener.onClickVoice() },
                onClickSend = {
                    uiState.listener.onClickSend(state.text.toString())
                    state.clearText()
                },
                selectedMedia = uiState.selectedMedia,
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
                        modifier = Modifier.padding(horizontal = ChatHorizontalPadding),
                        item = content,
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
) {
    AsyncImage(
        modifier = modifier
            .size(200.dp)
            .clip(MaterialTheme.shapes.small),
        contentScale = ContentScale.Crop,
        contentDescription = null,
        model = item.url,
    )
}
