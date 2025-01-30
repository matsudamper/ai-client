package net.matsudamper.gptclient

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.rememberNavController
import net.matsudamper.gptclient.navigation.Navigator
import net.matsudamper.gptclient.ui.ChatListUiState
import net.matsudamper.gptclient.ui.NewChatUiState
import net.matsudamper.gptclient.ui.SettingsScreenUiState
import net.matsudamper.gptclient.viewmodel.ChatViewModel
import net.matsudamper.gptclient.viewmodel.MainScreenViewModel
import net.matsudamper.gptclient.viewmodel.NewChatViewModel
import net.matsudamper.gptclient.viewmodel.SettingViewModel
import org.koin.java.KoinJavaComponent.getKoin

@Composable
internal fun App() {
    MaterialTheme {
        val navController = rememberNavController()
        val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
            "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
        }
        MainScreen(
            modifier = Modifier.fillMaxSize(),
            navController = navController,
            uiStateProvider = remember {
                object : UiStateProvider {
                    @Composable
                    override fun provideNewChatUiState(entry: NavBackStackEntry): NewChatUiState {
                        val viewModel = viewModel(entry) {
                            NewChatViewModel(
                                navControllerProvider = { navController },
                            )
                        }
                        return viewModel.uiState.collectAsState().value
                    }

                    @Composable
                    override fun provideChatUiState(
                        entry: NavBackStackEntry,
                        navigation: Navigator.Chat,
                    ): ChatListUiState {
                        val viewModel = viewModel(entry) {
                            val koin = getKoin()
                            ChatViewModel(
                                platformRequest = koin.get(),
                                settingDataStore = koin.get(),
                                openContext = navigation.openContext,
                                navControllerProvider = { navController },
                                appDatabase = koin.get(),
                            )
                        }
                        return viewModel.uiStateFlow.collectAsState().value
                    }

                    @Composable
                    override fun provideSettingUiState(entry: NavBackStackEntry): SettingsScreenUiState {
                        val viewModel = viewModel(entry) {
                            val koin = getKoin()
                            SettingViewModel(
                                settingDataStore = koin.get(),
                            )
                        }
                        return viewModel.uiStateFlow.collectAsState().value
                    }

                    @Composable
                    override fun provideMainScreenUiState(): MainScreenUiState {
                        val viewModel = viewModel(viewModelStoreOwner) {
                            MainScreenViewModel(
                                navControllerProvider = { navController },
                                appDatabase = getKoin().get(),
                            )
                        }

                        return viewModel.uiStateFlow.collectAsState().value
                    }
                }
            }
        )
    }
}
