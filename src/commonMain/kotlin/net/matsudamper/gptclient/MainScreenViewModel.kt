package net.matsudamper.gptclient

import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.matsudamper.gptclient.ui.MainScreenUiState
import net.matsudamper.gptclient.ui.Navigation

class MainScreenViewModel(
    private val navControllerProvider: () -> NavController,
) : ViewModel() {
    val uiStateFlow: StateFlow<MainScreenUiState> = MutableStateFlow(
        MainScreenUiState(
            listener = object : MainScreenUiState.Listener {
                override fun onClickSettings() {
                    navControllerProvider().navigate(
                        Navigation.Settings
                    )
                }
            }
        )
    ).also { uiState ->

    }
}