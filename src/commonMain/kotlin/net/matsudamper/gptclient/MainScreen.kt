package net.matsudamper.gptclient

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import java.time.Instant
import kotlinx.coroutines.launch
import compose.icons.FeatherIcons
import compose.icons.feathericons.MessageSquare
import net.matsudamper.gptclient.navigation.Navigator
import net.matsudamper.gptclient.ui.ChatList
import net.matsudamper.gptclient.ui.NewChat
import net.matsudamper.gptclient.ui.ProjectScreen
import net.matsudamper.gptclient.ui.SettingsScreen
import net.matsudamper.gptclient.ui.util.formatRelativeTime

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

    data class HistoryItem(
        val text: String,
        val projectName: String?,
        val updatedAt: Instant,
        val listener: HistoryItemListener,
    )

    @Immutable
    interface HistoryItemListener {
        fun onClick()
    }

    @Immutable
    interface Listener {
        fun onClickHome()
        fun onClickSettings()
        fun clearHistory()
    }
}

@Composable
public fun MainScreen(
    backStack: SnapshotStateList<Navigator>,
    uiStateProvider: UiStateProvider,
    modifier: Modifier = Modifier,
) {
    val rootUiState = uiStateProvider.provideMainScreenUiState()
    val coroutineScope = rememberCoroutineScope()

    var isVisibleSidePanel by remember { mutableStateOf(false) }
    var snapCloseSidePanel by remember { mutableStateOf(false) }
    var isAnimatingBackCancel by remember { mutableStateOf(false) }
    val panelOpenFraction = remember { Animatable(0f) }
    val sidePanelNavigationState = rememberNavigationEventState(
        currentInfo = NavigationEventInfo.None,
        backInfo = if (isVisibleSidePanel) listOf(NavigationEventInfo.None) else listOf(),
    )
    val sidePanelTransitionState = sidePanelNavigationState.transitionState
    LaunchedEffect(isVisibleSidePanel, sidePanelTransitionState, snapCloseSidePanel) {
        when (val state = sidePanelTransitionState) {
            is NavigationEventTransitionState.InProgress -> {
                if (!isAnimatingBackCancel) {
                    panelOpenFraction.snapTo(1f - state.latestEvent.progress)
                }
            }
            is NavigationEventTransitionState.Idle -> {
                when {
                    snapCloseSidePanel -> {
                        isAnimatingBackCancel = false
                        panelOpenFraction.snapTo(0f)
                        snapCloseSidePanel = false
                    }
                    isVisibleSidePanel && !isAnimatingBackCancel && panelOpenFraction.value <= 0f -> {
                        panelOpenFraction.animateTo(1f, tween<Float>(durationMillis = 250))
                    }
                    !isVisibleSidePanel -> {
                        panelOpenFraction.animateTo(0f, tween<Float>(durationMillis = 250))
                    }
                }
            }
        }
    }
    LaunchedEffect(backStack) {
        snapshotFlow { backStack.lastOrNull() }
            .collect {
                isVisibleSidePanel = false
            }
    }
    Surface(
        modifier = modifier,
    ) {
        BoxWithConstraints {
            val maxWidth = this.maxWidth
            Box {
                val panelWidth = 320.dp
                val offset = panelWidth * panelOpenFraction.value
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
                            backStack = backStack,
                            uiStateProvider = uiStateProvider,
                            onClickMenu = {
                                snapCloseSidePanel = false
                                isVisibleSidePanel = true
                            },
                        )
                    }
                    NavigationBackHandler(
                        state = sidePanelNavigationState,
                        isBackEnabled = isVisibleSidePanel,
                        onBackCancelled = {
                            coroutineScope.launch {
                                isAnimatingBackCancel = true
                                try {
                                    panelOpenFraction.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween<Float>(
                                            durationMillis = 500,
                                            easing = FastOutSlowInEasing,
                                        ),
                                    )
                                } finally {
                                    isAnimatingBackCancel = false
                                }
                            }
                        },
                        onBackCompleted = {
                            snapCloseSidePanel = true
                            isVisibleSidePanel = false
                        },
                    )
                    if (offset > 0.dp) {
                        val alpha = 0.4f * (offset / panelWidth).coerceIn(0f, 1f)
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
    backStack: SnapshotStateList<Navigator>,
    uiStateProvider: UiStateProvider,
    onClickMenu: () -> Unit,
) {
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = { key ->
            when (key) {
                is Navigator.StartChat -> NavEntry(key) {
                    val uiState = uiStateProvider.provideNewChatUiState()
                    NewChat(
                        modifier = Modifier.fillMaxSize(),
                        uiState = uiState,
                        onClickMenu = { onClickMenu() },
                    )
                }
                is Navigator.Chat -> NavEntry(key) {
                    val uiState = uiStateProvider.provideChatUiState(navigator = key)
                    ChatList(
                        modifier = Modifier.fillMaxSize(),
                        uiState = uiState,
                        onClickMenu = { onClickMenu() },
                    )
                }
                is Navigator.Settings -> NavEntry(key) {
                    val uiState = uiStateProvider.provideSettingUiState()
                    SettingsScreen(
                        modifier = Modifier.fillMaxSize(),
                        uiState = uiState,
                        onClickMenu = { onClickMenu() },
                    )
                }
                is Navigator.Project -> NavEntry(key) {
                    val uiState = uiStateProvider.provideProjectUiState(navigator = key)
                    ProjectScreen(
                        modifier = Modifier.fillMaxSize(),
                        uiState = uiState,
                        onClickMenu = { onClickMenu() },
                    )
                }
            }
        },
    )
}

@Composable
private fun SidePanel(
    onClickSettings: () -> Unit,
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
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = formatRelativeTime(item.updatedAt),
                                maxLines = 1,
                                style = MaterialTheme.typography.labelSmall,
                            )
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
                .navigationBarsPadding()
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(onClick = onClickSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                )
            }
        }
    }
}
