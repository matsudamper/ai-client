package net.matsudamper.gptclient.viewmodel

import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.matsudamper.gptclient.ui.BuiltinProjectUiState

class BuiltinProjectViewModel(
    initialProjectName: String,
    navControllerProvider: () -> NavHostController
) : ViewModel() {
    val uiStateFlow: StateFlow<BuiltinProjectUiState> = MutableStateFlow(
        BuiltinProjectUiState(
            projectName = initialProjectName,
            state = BuiltinProjectUiState.LoadingState.Loading,
        )
    ).also { uiState ->

    }
}