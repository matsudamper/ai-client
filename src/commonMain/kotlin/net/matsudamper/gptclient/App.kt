package net.matsudamper.gptclient

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.rememberNavController
import net.matsudamper.gptclient.gpt.ChatGptClient
import net.matsudamper.gptclient.navigation.Navigator
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
internal fun App() {
    MaterialTheme {
        val navController = rememberNavController()
        val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
            "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
        }
        val platformRequest = remember {
            val koin = getKoin()
            koin.get<PlatformRequest>()
        }

        LaunchedEffect(platformRequest) {
            platformRequest.setNotificationLaunchHandler { chatRoomId ->
                try {
                    val roomId = net.matsudamper.gptclient.room.entity.ChatRoomId(chatRoomId.toLong())
                    navController.navigate(
                        Navigator.Chat(
                            Navigator.Chat.ChatOpenContext.OpenChat(roomId),
                        ),
                    ) {
                        popUpTo(navController.graph.startDestinationRoute!!) {
                            inclusive = false
                        }
                    }
                } catch (e: Throwable) {
                    throw e
                }
            }
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
                                platformRequest = getKoin().get(),
                                appDatabase = getKoin().get(),
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
                                openContext = navigation.openContext,
                                insertDataAndAddRequestUseCase = createInsertDataAndAddRequestUseCase(),
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
                        entry: NavBackStackEntry,
                        navigator: Navigator.Project,
                    ): ProjectUiState {
                        val viewModel = viewModel(
                            viewModelStoreOwner = viewModelStoreOwner,
                            key = navigator.type.toString(),
                        ) {
                            ProjectViewModel(
                                navControllerProvider = { navController },
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
