package net.matsudamper.gptclient

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry

@Composable
internal fun App() {
    MaterialTheme {
        MainScreen(
            modifier = Modifier.fillMaxSize(),
            uiStateProvider = remember {
                object : UiStateProvider {
                    @Composable
                    override fun provideNewChatUiState(entry: NavBackStackEntry): NewChatUiState {
                        val viewModel = viewModel(entry) { NewChatViewModel() }
                        return viewModel.uiState.collectAsState().value
                    }
                }
            }
        )
//                ChatList(
//                    modifier = Modifier.fillMaxSize(),
//                    uiState = ChatListUiState(
//                        items = listOf(
//                            ChatListUiState.Item.Agent("Hello!"),
//                            ChatListUiState.Item.User("Hello!"),
//                        ),
//                        listener = object : ChatListUiState.Listener {
//                            override fun onClickImage() {
//                            }
//
//                            override fun onClickVoice() {
//                            }
//                        }
//                    )
//                )
    }
}
