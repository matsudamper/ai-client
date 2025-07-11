package net.matsudamper.gptclient

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import compose.icons.FeatherIcons
import compose.icons.feathericons.MessageSquare
import net.matsudamper.gptclient.navigation.Navigator
import net.matsudamper.gptclient.ui.ChatList
import net.matsudamper.gptclient.ui.NewChat
import net.matsudamper.gptclient.ui.ProjectScreen
import net.matsudamper.gptclient.ui.SettingsScreen
import net.matsudamper.gptclient.ui.platform.BackHandler

object MainScreenTestTag {
    val navigationMenu = "navigation_menu"
    val settingsButton = "settings_button"
    val homeButton = "home_button"
    val clearHistoryButton = "clear_history_button"
    fun historyItem(index: Int) = "history_item_$index"
}

data class MainScreenUiState(val history: History, val listener: Listener) {
    sealed interface History {
        data object Loading : History
        data class Loaded(val items: List<HistoryItem>) : History
    }

    data class HistoryItem(val text: String, val projectName: String?, val listener: HistoryItemListener)

    @Immutable
    interface HistoryItemListener {
        fun onClick()
    }

    @Immutable
    interface Listener {
        fun onClickHome()
        fun onClickSettings()
        fun onClickUsage()
        fun clearHistory()
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
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow
            .collect {
                isVisibleSidePanel = false
            }
    }
    Surface(
        modifier = modifier,
    ) {
        BoxWithConstraints {
            val maxWidth = maxWidth
            Box {
                val panelWidth = 320.dp
                val offset by animateDpAsState(
                    targetValue = if (isVisibleSidePanel) panelWidth else 0.dp,
                    animationSpec = tween(durationMillis = 250),
                )
                SidePanel(
                    modifier = Modifier.fillMaxHeight()
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(
                                constraints.copy(
                                    minWidth = panelWidth.roundToPx(),
                                    maxWidth = panelWidth.roundToPx(),
                                ),
                            )
                            layout(placeable.width, placeable.height) {
                                placeable.place((-panelWidth + offset).roundToPx(), 0)
                            }
                        },
                    onClickSettings = { rootUiState.listener.onClickSettings() },
                    onClickUsage = { rootUiState.listener.onClickUsage() },
                    onClickHome = { rootUiState.listener.onClickHome() },
                    historyClear = { rootUiState.listener.clearHistory() },
                    history = rootUiState.history,
                )
                Box(
                    modifier = Modifier.fillMaxHeight()
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(
                                constraints.copy(
                                    minWidth = maxWidth.roundToPx(),
                                    maxWidth = maxWidth.roundToPx(),
                                ),
                            )
                            layout(placeable.width, placeable.height) {
                                placeable.place(offset.roundToPx(), 0)
                            }
                        },
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                    ) {
                        Navigation(
                            enableNavigationBack = isVisibleSidePanel.not(),
                            navController = navController,
                            uiStateProvider = uiStateProvider,
                            onClickMenu = { isVisibleSidePanel = true },
                            requestBack = { isVisibleSidePanel = false },
                        )
                    }
                    val alpha by animateFloatAsState(if (isVisibleSidePanel) 0.4f else 0f, tween(250))
                    if (isVisibleSidePanel) {
                        Box(
                            modifier = Modifier.fillMaxSize()
                                .background(Color.Black.copy(alpha = alpha))
                                .clickable(
                                    interactionSource = null,
                                    indication = null,
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
    enableNavigationBack: Boolean,
    requestBack: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = Navigator.StartChat,
    ) {
        composable<Navigator.StartChat> {
            val uiState = uiStateProvider.provideNewChatUiState(entry = it)
            NewChat(
                modifier = Modifier.fillMaxSize(),
                uiState = uiState,
                onClickMenu = { onClickMenu() },
            )
        }
        composable<Navigator.Chat>(
            typeMap = Navigator.Chat.typeMap,
        ) {
            val navigatorItem = it.toRoute<Navigator.Chat>()
            val uiState = uiStateProvider.provideChatUiState(entry = it, navigator = navigatorItem)

            ChatList(
                modifier = Modifier.fillMaxSize(),
                uiState = uiState,
                onClickMenu = { onClickMenu() },
            )
        }
        composable<Navigator.Settings> {
            val uiState = uiStateProvider.provideSettingUiState(entry = it)
            SettingsScreen(
                modifier = Modifier.fillMaxSize(),
                uiState = uiState,
                onClickMenu = { onClickMenu() },
            )
        }
        composable<Navigator.Project>(
            typeMap = Navigator.Project.typeMap,
        ) {
            val navigatorItem = it.toRoute<Navigator.Project>()
            val uiState = uiStateProvider.provideProjectUiState(entry = it, navigator = navigatorItem)

            ProjectScreen(
                modifier = Modifier.fillMaxSize(),
                uiState = uiState,
                onClickMenu = { onClickMenu() },
            )
        }
    }
    BackHandler(enableNavigationBack.not()) { requestBack() }
}

@Composable
private fun SidePanel(
    onClickSettings: () -> Unit,
    onClickUsage: () -> Unit,
    onClickHome: () -> Unit,
    historyClear: () -> Unit,
    history: MainScreenUiState.History,
    modifier: Modifier = Modifier,
) {
    var visibleHistoryDeleteDialog by remember { mutableStateOf(false) }
    if (visibleHistoryDeleteDialog) {
        AlertDialog(
            onDismissRequest = { visibleHistoryDeleteDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    visibleHistoryDeleteDialog = false
                    historyClear()
                }) {
                    Text("削除する")
                }
            },
            dismissButton = {
                TextButton(onClick = { visibleHistoryDeleteDialog = false }) {
                    Text("キャンセル")
                }
            },
            title = {
                Text("履歴を削除しますか？")
            },
        )
    }
    Column(
        modifier = modifier.statusBarsPadding(),
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onClickHome()
                }
                .padding(24.dp),
            text = "Home",
        )
        HorizontalDivider()
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "履歴",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { visibleHistoryDeleteDialog = true }) {
                Text("クリア")
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth()
                .weight(1f),
        ) {
            when (history) {
                is MainScreenUiState.History.Loaded -> {
                    items(history.items) { item ->
                        Row(
                            modifier = Modifier.clickable { item.listener.onClick() }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = FeatherIcons.MessageSquare,
                                contentDescription = null,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center,
                            ) {
                                if (item.projectName != null) {
                                    Text(
                                        modifier = Modifier.fillMaxWidth(),
                                        text = item.projectName,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = item.text,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }

                is MainScreenUiState.History.Loading -> {
                    item {
                        Box(
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer)
                .navigationBarsPadding(),
        ) {
            TextButton(onClick = { onClickUsage() }) {
                Text("Usage")
            }
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
