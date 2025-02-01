package net.matsudamper.gptclient.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.MessageSquare
import net.matsudamper.gptclient.ui.component.ChatFooter

data class BuiltinProjectUiState(
    val projectName: String,
    val selectedMedia: List<String>,
    val systemMessage: SystemMessage,
    val visibleMediaLoading: Boolean,
    val chatRoomsState: ChatRoomsState,
    val listener: Listener,
) {
    sealed interface ChatRoomsState {
        object Loading : ChatRoomsState
        data class Loaded(
            val histories: List<History>,
        ) : ChatRoomsState
    }

    data class SystemMessage(
        val text: String,
        val editable: Boolean,
    )

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
fun BuiltinProject(
    uiState: BuiltinProjectUiState,
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
                Text(text = uiState.projectName)
            }
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(
                horizontal = 12.dp,
            )
        ) {
            when (uiState.chatRoomsState) {
                is BuiltinProjectUiState.ChatRoomsState.Loaded -> {
                    item {
                        Text("命令")
                        Text(uiState.systemMessage.text)
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                    item {
                        Text("履歴")
                    }
                    items(uiState.chatRoomsState.histories) { history ->
                        Row(
                            modifier = Modifier.fillMaxSize()
                                .clickable {
                                    history.listener.onClick()
                                }
                                .padding(12.dp),
                        ) {
                            Icon(
                                imageVector = FeatherIcons.MessageSquare,
                                contentDescription = null,
                            )
                            Text(history.text)
                        }
                    }
                }

                is BuiltinProjectUiState.ChatRoomsState.Loading -> {
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
