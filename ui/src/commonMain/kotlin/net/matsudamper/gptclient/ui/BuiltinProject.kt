package net.matsudamper.gptclient.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class BuiltinProjectUiState(
    val projectName: String,
    val state: LoadingState,
) {
    sealed interface LoadingState {
        object Loading : LoadingState
        data class Loaded(
            val uni: Unit = Unit,
        ) : LoadingState
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuiltinProject(
    uiState: BuiltinProjectUiState,
    onClickMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .navigationBarsPadding()
            .imePadding(),
    ) {
        TopAppBar(
            modifier = Modifier,
            navigationIcon = {
                IconButton(onClick = { onClickMenu() }) {
                    Icon(imageVector = Icons.Default.Menu, contentDescription = null)
                }
            },
            title = {
                Text(text = uiState.projectName)
            }
        )
    }
}
