package net.matsudamper.gptclient

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import net.matsudamper.gptclient.client.openai.ChatGptClient
import net.matsudamper.gptclient.navigation.AppNavigator
import net.matsudamper.gptclient.navigation.Navigator
import net.matsudamper.gptclient.room.entity.ChatRoomId
import net.matsudamper.gptclient.ui.ChatListUiState
import net.matsudamper.gptclient.ui.NewChatUiState
import net.matsudamper.gptclient.ui.ProjectUiState
import net.matsudamper.gptclient.ui.SettingsScreenUiState
import net.matsudamper.gptclient.usecase.DeleteChatRoomUseCase
import net.matsudamper.gptclient.viewmodel.AddRequestUseCase
import net.matsudamper.gptclient.viewmodel.ChatViewModel
import net.matsudamper.gptclient.viewmodel.MainScreenViewModel
import net.matsudamper.gptclient.viewmodel.NewChatViewModel
import net.matsudamper.gptclient.viewmodel.ProjectViewModel
import net.matsudamper.gptclient.viewmodel.SettingViewModel
import org.koin.java.KoinJavaComponent.getKoin

@Composable
fun App(initialChatRoomId: ChatRoomId? = null) {
    MaterialTheme {
        val backStack = remember {
            mutableStateListOf<Navigator>(Navigator.StartChat).apply {
                if (initialChatRoomId != null) {
                    add(
                        Navigator.Chat(
                            openContext = Navigator.Chat.ChatOpenContext.OpenChat(initialChatRoomId),
                        ),
                    )
                }
            }
        }
        val appNavigator = remember(backStack) { AppNavigator(backStack) }
        val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
            "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
        }

        MainScreen(
            modifier = Modifier.fillMaxSize(),
            backStack = backStack,
            uiStateProvider = remember(appNavigator) {
                object : UiStateProvider {
                    @Composable
                    override fun provideNewChatUiState(): NewChatUiState {
                        val viewModel = viewModel {
                            NewChatViewModel(
                                appNavigator = appNavigator,
                                platformRequest = getKoin().get(),
                                appDatabase = getKoin().get(),
                            )
                        }
                        return viewModel.uiState.collectAsState().value
                    }

                    @Composable
                    override fun provideChatUiState(
                        navigator: Navigator.Chat,
                    ): ChatListUiState {
                        val viewModel = viewModel {
                            val koin = getKoin()
                            ChatViewModel(
                                platformRequest = koin.get(),
                                openContext = navigator.openContext,
                                insertDataAndAddRequestUseCase = createInsertDataAndAddRequestUseCase(),
                                appDatabase = koin.get(),
                            )
                        }
                        return viewModel.uiStateFlow.collectAsState().value
                    }

                    @Composable
                    override fun provideSettingUiState(): SettingsScreenUiState {
                        val viewModel = viewModel {
                            val koin = getKoin()
                            SettingViewModel(
                                settingDataStore = koin.get(),
                                platformRequest = koin.get(),
                            )
                        }
                        return viewModel.uiStateFlow.collectAsState().value
                    }

                    @Composable
                    override fun provideMainScreenUiState(): MainScreenUiState {
                        val viewModel = viewModel(viewModelStoreOwner) {
                            MainScreenViewModel(
                                appNavigator = appNavigator,
                                appDatabase = getKoin().get(),
                                platformRequest = getKoin().get(),
                                deleteChatRoomUseCase = DeleteChatRoomUseCase(
                                    appDatabase = getKoin().get(),
                                    platformRequest = getKoin().get(),
                                ),
                            )
                        }

                        return viewModel.uiStateFlow.collectAsState().value
                    }

                    @Composable
                    override fun provideProjectUiState(
                        navigator: Navigator.Project,
                    ): ProjectUiState {
                        val viewModel = viewModel(
                            viewModelStoreOwner = viewModelStoreOwner,
                            key = navigator.type.toString(),
                        ) {
                            ProjectViewModel(
                                appNavigator = appNavigator,
                                navigator = navigator,
                                platformRequest = getKoin().get(),
                                appDatabase = getKoin().get(),
                            )
                        }

                        return viewModel.uiStateFlow.collectAsState().value
                    }

                    private fun createInsertDataAndAddRequestUseCase(): AddRequestUseCase {
                        val koin = getKoin()
                        return AddRequestUseCase(
                            appDatabase = koin.get(),
                            platformRequest = koin.get(),
                            gptClientProvider = { secretKey -> ChatGptClient(secretKey) },
                            settingDataStore = koin.get(),
                            workManagerScheduler = koin.get(),
                        )
                    }
                }
            },
        )
    }
}
