package net.matsudamper.gptclient.viewmodel

import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.matsudamper.gptclient.MainScreenUiState
import net.matsudamper.gptclient.navigation.Navigator

class MainScreenViewModel(
    private val navControllerProvider: () -> NavController,
) : ViewModel() {
    val uiStateFlow: StateFlow<MainScreenUiState> = MutableStateFlow(
        MainScreenUiState(
            listener = object : MainScreenUiState.Listener {
                override fun onClickSettings() {
                    navControllerProvider().navigate(
                        Navigator.Settings
                    )
                }
            }
        )
    ).also { uiState ->

    }
}