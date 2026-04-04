package net.matsudamper.gptclient

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import net.matsudamper.gptclient.datastore.SettingDataStore
import net.matsudamper.gptclient.datastore.ThemeMode
import net.matsudamper.gptclient.navigation.Navigator
import net.matsudamper.gptclient.ui.ChatListUiState
import net.matsudamper.gptclient.ui.NewChatUiState
import net.matsudamper.gptclient.ui.ProjectUiState
import net.matsudamper.gptclient.ui.SettingsScreenUiState
import net.matsudamper.gptclient.usecase.DeleteChatRoomUseCase
import net.matsudamper.gptclient.viewmodel.AddRequestUseCase
import net.matsudamper.gptclient.viewmodel.AppNavigationViewModel
import net.matsudamper.gptclient.viewmodel.ChatViewModel
import net.matsudamper.gptclient.viewmodel.MainScreenViewModel
import net.matsudamper.gptclient.viewmodel.NewChatViewModel
import net.matsudamper.gptclient.viewmodel.ProjectViewModel
import net.matsudamper.gptclient.viewmodel.SettingViewModel
import org.koin.java.KoinJavaComponent.getKoin

@Composable
fun App(
    launchNavigationRequest: LaunchNavigationRequest = LaunchNavigationRequest.none(),
    providePlatformRequest: () -> PlatformRequest,
) {
    val settingDataStore: SettingDataStore = remember { getKoin().get() }
    val themeMode = settingDataStore.getThemeModeFlow()
        .collectAsState(initial = ThemeMode.SYSTEM).value
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val lightColors = lightColorScheme(
        primary = Color(0xFF5A46C8),
        surfaceVariant = Color(0xFFF1F0F8),
        secondaryContainer = Color(0xFFE8E4F8),
    )
    val darkColors = darkColorScheme(
        primary = Color(0xFFC5B7FF),
        onPrimary = Color(0xFF2A176F),
        surface = Color(0xFF111018),
        onSurface = Color(0xFFF2F0FA),
        surfaceVariant = Color(0xFF2A2835),
        onSurfaceVariant = Color(0xFFE7E1F7),
        secondaryContainer = Color(0xFF47435A),
        onSecondaryContainer = Color(0xFFF2EEFF),
    )

    MaterialTheme(
        colorScheme = if (isDark) darkColors else lightColors,
    ) {
        val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
            "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
        }
        val appNavigationViewModel = viewModel(viewModelStoreOwner) {
            AppNavigationViewModel()
        }
        val backStack = appNavigationViewModel.backStack
        val appNavigator = appNavigationViewModel.appNavigator

        LaunchedEffect(launchNavigationRequest.id) {
            launchNavigationRequest.navigator?.let { navigator ->
                appNavigator.navigateClearToStart(navigator)
            }
        }

        MainScreen(
            modifier = Modifier.fillMaxSize(),
            backStack = backStack,
            uiStateProvider = remember(appNavigator) {
                object : UiStateProvider {
                    @Composable
                    override fun provideNewChatUiState(): NewChatUiState {
                        val viewModel = viewModel {
                            val koin = getKoin()
                            NewChatViewModel(
                                appNavigator = appNavigator,
                                appDatabase = koin.get(),
                                settingDataStore = koin.get(),
                                localModelRepository = koin.get(),
                            )
                        }
                        LaunchedEffect(viewModel, providePlatformRequest) {
                            viewModel.eventHandler.collect(
                                object : NewChatViewModel.Event {
                                    override fun providePlatformRequest(): PlatformRequest = providePlatformRequest()
                                },
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
                                openContext = navigator.openContext,
                                insertDataAndAddRequestUseCase = createInsertDataAndAddRequestUseCase(),
                                appDatabase = koin.get(),
                                localModelRepository = koin.get(),
                            )
                        }
                        LaunchedEffect(viewModel, providePlatformRequest) {
                            viewModel.eventHandler.collect(
                                object : ChatViewModel.Event {
                                    override fun providePlatformRequest(): PlatformRequest = providePlatformRequest()
                                },
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
                                localModelRepository = koin.get(),
                            )
                        }
                        LaunchedEffect(viewModel, providePlatformRequest) {
                            viewModel.eventHandler.collect(
                                object : SettingViewModel.Event {
                                    override fun providePlatformRequest(): PlatformRequest = providePlatformRequest()
                                },
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
                                deleteChatRoomUseCase = DeleteChatRoomUseCase(
                                    appDatabase = getKoin().get(),
                                ),
                            )
                        }
                        LaunchedEffect(viewModel, providePlatformRequest) {
                            viewModel.eventHandler.collect(
                                object : MainScreenViewModel.Event {
                                    override fun providePlatformRequest(): PlatformRequest = providePlatformRequest()
                                },
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
                            val koin = getKoin()
                            ProjectViewModel(
                                appNavigator = appNavigator,
                                navigator = navigator,
                                appDatabase = koin.get(),
                                settingDataStore = koin.get(),
                                localModelRepository = koin.get(),
                            )
                        }
                        LaunchedEffect(viewModel, providePlatformRequest) {
                            viewModel.eventHandler.collect(
                                object : ProjectViewModel.Event {
                                    override fun providePlatformRequest(): PlatformRequest = providePlatformRequest()
                                },
                            )
                        }

                        return viewModel.uiStateFlow.collectAsState().value
                    }

                    private fun createInsertDataAndAddRequestUseCase(): AddRequestUseCase {
                        val koin = getKoin()
                        return AddRequestUseCase(
                            appDatabase = koin.get(),
                            workManagerScheduler = koin.get(),
                        )
                    }
                }
            },
        )
    }
}
