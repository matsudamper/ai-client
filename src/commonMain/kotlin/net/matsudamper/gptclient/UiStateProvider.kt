package net.matsudamper.gptclient

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.navigation.NavBackStackEntry
import net.matsudamper.gptclient.navigation.Navigator
import net.matsudamper.gptclient.ui.BuiltinProjectUiState
import net.matsudamper.gptclient.ui.ChatListUiState
import net.matsudamper.gptclient.ui.NewChatUiState
import net.matsudamper.gptclient.ui.SettingsScreenUiState


@Immutable
interface UiStateProvider {
    @Composable
    fun provideNewChatUiState(entry: NavBackStackEntry): NewChatUiState

    @Composable
    fun provideChatUiState(entry: NavBackStackEntry, navigator: Navigator.Chat): ChatListUiState

    @Composable
    fun provideCalendarChatUiState(entry: NavBackStackEntry, navigator: Navigator.CalendarChat): ChatListUiState

    @Composable
    fun provideSettingUiState(entry: NavBackStackEntry): SettingsScreenUiState

    @Composable
    fun provideMainScreenUiState(): MainScreenUiState

    @Composable
    fun provideBuiltinProjectUiState(entry: NavBackStackEntry, navigator: Navigator.BuiltinProject): BuiltinProjectUiState
}
