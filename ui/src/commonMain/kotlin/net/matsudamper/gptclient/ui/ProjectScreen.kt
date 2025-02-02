package net.matsudamper.gptclient.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.MessageSquare
import net.matsudamper.gptclient.ui.component.ChatFooter

data class ProjectUiState(
    val projectName: String,
    val selectedMedia: List<String>,
    val systemMessage: SystemMessage,
    val visibleMediaLoading: Boolean,
    val chatRoomsState: ChatRoomsState,
    val modelState: ModelState,
    val listener: Listener,
) {
    data class ModelState(
        val selectedModel: String,
        val models: List<Item>
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectScreen(
    uiState: ProjectUiState,
    onClickMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
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
        )
        val itemHorizontalPadding = 12.dp
        LazyColumn(
            modifier = Modifier.fillMaxWidth()
                .weight(1f),
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
                            Text(history.text)
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
        val state = rememberTextFieldState()
        ChatFooter(
            modifier = Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .navigationBarsPadding(),
            textFieldState = state,
            onClickImage = { uiState.listener.selectMedia() },
            onClickVoice = { uiState.listener.recordVoice() },
            selectedMedia = uiState.selectedMedia,
            visibleMediaLoading = uiState.visibleMediaLoading,
            onClickSend = {
                uiState.listener.send(state.text.toString())
                state.clearText()
            },
        )
    }
}
