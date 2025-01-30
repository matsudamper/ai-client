package net.matsudamper.gptclient.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.unit.dp
import net.matsudamper.gptclient.ui.component.ChatFooter

data class ChatListUiState(
    val items: List<Item> = listOf(),
    val listener: Listener,
) {
    sealed interface Item {
        data class Agent(val message: String) : Item
        data class User(val message: String) : Item
    }

    @Immutable
    interface Listener {
        fun onClickImage()
        fun onClickVoice()
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
                when (item) {
                    is ChatListUiState.Item.Agent -> {
                        AgentItem(
                            modifier = Modifier.fillMaxWidth(),
                            item = item,
                        )
                    }

                    is ChatListUiState.Item.User -> {
                        UserItem(
                            modifier = Modifier.fillMaxWidth(),
                            item = item,
                        )
                    }
                }
            }
        }
        val state = rememberTextFieldState()
        ChatFooter(
            modifier = Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .navigationBarsPadding(),
            state = state,
            onClickImage = { uiState.listener.onClickImage() },
            onClickVoice = { uiState.listener.onClickVoice() },
            onClickSend = {}
        )
    }
}

@Composable
private fun AgentItem(
    item: ChatListUiState.Item.Agent,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.width(24.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(6.dp),
                text = item.message,
            )
        }
    }
}

@Composable
private fun UserItem(
    item: ChatListUiState.Item.User,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(6.dp),
                text = item.message,
            )
        }
    }
    Spacer(modifier = Modifier.width(24.dp))
}
