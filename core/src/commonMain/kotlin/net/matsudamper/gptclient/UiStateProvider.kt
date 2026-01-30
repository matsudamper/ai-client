package net.matsudamper.gptclient

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.navigation.NavBackStackEntry
import net.matsudamper.gptclient.navigation.Navigator
import net.matsudamper.gptclient.ui.ChatListUiState
import net.matsudamper.gptclient.ui.NewChatUiState
import net.matsudamper.gptclient.ui.ProjectUiState
import net.matsudamper.gptclient.ui.SettingsScreenUiState

@Immutable
interface UiStateProvider {
    @Composable
    fun provideNewChatUiState(entry: NavBackStackEntry): NewChatUiState

    @Composable
    fun provideChatUiState(entry: NavBackStackEntry, navigator: Navigator.Chat): ChatListUiState

    @Composable
    fun provideSettingUiState(entry: NavBackStackEntry): SettingsScreenUiState

    @Composable
    fun provideMainScreenUiState(): MainScreenUiState

    @Composable
    fun provideProjectUiState(entry: NavBackStackEntry, navigator: Navigator.Project): ProjectUiState
}
