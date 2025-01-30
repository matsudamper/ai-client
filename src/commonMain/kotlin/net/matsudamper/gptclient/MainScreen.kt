package net.matsudamper.gptclient

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import net.matsudamper.gptclient.room.entity.ChatRoomId
import net.matsudamper.gptclient.ui.ChatList
import net.matsudamper.gptclient.ui.ChatListUiState
import net.matsudamper.gptclient.ui.NewChat
import net.matsudamper.gptclient.ui.NewChatUiState
import net.matsudamper.gptclient.ui.SettingsScreen
import net.matsudamper.gptclient.ui.SettingsScreenUiState
import net.matsudamper.gptclient.ui.platform.BackHandler

@Immutable
sealed interface Navigation {
    @Serializable
    data object StartChat : Navigation

    @Serializable
    data class Chat(val openContext: OpenContext) : Navigation {
        sealed interface OpenContext {
            data class NewMessage(val initialMessage: String) : OpenContext
            data class OpenChat(val chatRoomId: ChatRoomId) : OpenContext
        }
    }

    @Serializable
    data object Settings : Navigation
}

@Immutable
interface UiStateProvider {
    @Composable
    fun provideNewChatUiState(entry: NavBackStackEntry): NewChatUiState

    @Composable
    fun provideChatUiState(entry: NavBackStackEntry, navigation: Navigation.Chat): ChatListUiState

    @Composable
    fun provideSettingUiState(entry: NavBackStackEntry): SettingsScreenUiState

    @Composable
    fun provideMainScreenUiState(): MainScreenUiState
}

data class MainScreenUiState(
    val listener: Listener,
) {
    @Immutable
    interface Listener {
        fun onClickSettings()
    }
}

@Composable
public fun MainScreen(
    navController: NavHostController,
    uiStateProvider: UiStateProvider,
    modifier: Modifier = Modifier,
) {
    val rootUiState = uiStateProvider.provideMainScreenUiState()

    var isVisibleSidePanel by remember { mutableStateOf(false) }
    BackHandler(isVisibleSidePanel) {
        isVisibleSidePanel = false
    }
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow
            .collect {
                isVisibleSidePanel = false
            }
    }
    Surface(
        modifier = modifier
    ) {
        BoxWithConstraints {
            val maxWidth = maxWidth
            Box {
                val panelWidth = 320.dp
                val offset by animateDpAsState(
                    targetValue = if (isVisibleSidePanel) panelWidth else 0.dp,
                    animationSpec = tween(durationMillis = 250)
                )
                SidePanel(
                    modifier = Modifier.fillMaxHeight()
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(
                                constraints.copy(
                                    minWidth = panelWidth.roundToPx(),
                                    maxWidth = panelWidth.roundToPx(),
                                )
                            )
                            layout(placeable.width, placeable.height) {
                                placeable.place((-panelWidth + offset).roundToPx(), 0)
                            }
                        },
                    onClickSettings = { rootUiState.listener.onClickSettings() },
                )
                Box(
                    modifier = Modifier.fillMaxHeight()
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(
                                constraints.copy(
                                    minWidth = maxWidth.roundToPx(),
                                    maxWidth = maxWidth.roundToPx(),
                                )
                            )
                            layout(placeable.width, placeable.height) {
                                placeable.place(offset.roundToPx(), 0)
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Navigation(
                            navController = navController,
                            uiStateProvider = uiStateProvider,
                            onClickMenu = { isVisibleSidePanel = true },
                        )
                    }
                    val alpha by animateFloatAsState(if (isVisibleSidePanel) 0.4f else 0f, tween(250))
                    if (isVisibleSidePanel) {
                        Box(
                            modifier = Modifier.fillMaxSize()
                                .background(Color.Black.copy(alpha = alpha))
                                .clickable(
                                    interactionSource = null,
                                    indication = null
                                ) { isVisibleSidePanel = false },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Navigation(
    navController: NavHostController,
    uiStateProvider: UiStateProvider,
    onClickMenu: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = Navigation.StartChat,
    ) {
        composable<Navigation.StartChat> {
            val uiState = uiStateProvider.provideNewChatUiState(entry = it)
            NewChat(
                modifier = Modifier.fillMaxSize(),
                uiState = uiState,
                onClickMenu = { onClickMenu() },
            )
        }
        composable<Navigation.Chat> {
            val navigationItem = it.toRoute<Navigation.Chat>()
            val uiState = uiStateProvider.provideChatUiState(entry = it, navigation = navigationItem)

            ChatList(
                modifier = Modifier.fillMaxSize(),
                uiState = uiState,
                onClickMenu = { onClickMenu() },
            )
        }
        composable<Navigation.Settings> {
            val uiState = uiStateProvider.provideSettingUiState(entry = it)
            SettingsScreen(
                modifier = Modifier.fillMaxSize(),
                uiState = uiState,
                onClickMenu = { onClickMenu() }
            )
        }
    }
}

@Composable
private fun SidePanel(
    onClickSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {

                }
                .padding(24.dp),
            text = "New Chat",
        )
        Spacer(Modifier.weight(1f))
        Row {
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onClickSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                )
            }
        }
    }
}
