package net.matsudamper.gptclient

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import net.matsudamper.gptclient.navigation.Navigator
import net.matsudamper.gptclient.ui.ChatListUiState
import net.matsudamper.gptclient.ui.NewChatUiState
import net.matsudamper.gptclient.ui.ProjectUiState
import net.matsudamper.gptclient.ui.SettingsScreenUiState

@Immutable
interface UiStateProvider {
    @Composable
    fun provideNewChatUiState(): NewChatUiState

    @Composable
    fun provideChatUiState(navigator: Navigator.Chat): ChatListUiState

    @Composable
    fun provideSettingUiState(): SettingsScreenUiState

    @Composable
    fun provideMainScreenUiState(): MainScreenUiState

    @Composable
    fun provideProjectUiState(navigator: Navigator.Project): ProjectUiState
}
