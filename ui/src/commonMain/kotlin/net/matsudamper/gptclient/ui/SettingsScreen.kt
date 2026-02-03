package net.matsudamper.gptclient.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

sealed interface SettingsScreenUiState {
    data object Loading : SettingsScreenUiState
    data class Loaded(
        val initialSecretKey: String,
        val initialGeminiSecretKey: String,
        val listener: Listener,
    ) : SettingsScreenUiState {
        @Immutable
        interface Listener {
            fun updateSecretKey(text: String)
            fun updateGeminiSecretKey(text: String)
            fun onClickOpenAiUsage()
            fun onClickGeminiUsage()
        }
    }
}
private val HorizontalPadding = 12.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun SettingsScreen(
    uiState: SettingsScreenUiState,
    onClickMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text("設定")
                },
                navigationIcon = {
                    IconButton(onClick = { onClickMenu() }) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
            )
            when (uiState) {
                is SettingsScreenUiState.Loading -> {
                    Loading(
                        modifier = Modifier.fillMaxWidth()
                            .weight(1f),
                    )
                }

                is SettingsScreenUiState.Loaded -> {
                    Loaded(
                        uiState = uiState,
                        modifier = Modifier.fillMaxWidth()
                            .weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun Loading(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier,
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun Loaded(
    uiState: SettingsScreenUiState.Loaded,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
    ) {
        ApiKeySettingItem(
            modifier = Modifier.fillMaxWidth(),
            title = "OpenAI Secret Key",
            initialValue = uiState.initialSecretKey,
            onValueChange = { uiState.listener.updateSecretKey(it) },
        )
        OutlinedButton(
            modifier = Modifier.align(Alignment.End)
                .padding(horizontal = HorizontalPadding),
            onClick = { uiState.listener.onClickOpenAiUsage() },
        ) {
            Text("Usage")
        }
        Spacer(modifier = Modifier.height(12.dp))
        ApiKeySettingItem(
            modifier = Modifier.fillMaxWidth(),
            title = "Gemini API Key",
            initialValue = uiState.initialGeminiSecretKey,
            onValueChange = { uiState.listener.updateGeminiSecretKey(it) },
        )
        OutlinedButton(
            modifier = Modifier.align(Alignment.End)
                .padding(horizontal = HorizontalPadding),
            onClick = { uiState.listener.onClickGeminiUsage() },
        ) {
            Text("Usage")
        }
    }
}

@Composable
private fun ApiKeySettingItem(
    modifier: Modifier = Modifier,
    title: String,
    initialValue: String,
    onValueChange: (String) -> Unit,
) {
    SettingItem(
        modifier = modifier,
        title = {
            Text(title)
        },
        content = {
            val state = rememberTextFieldState(initialValue)
            LaunchedEffect(state.text) {
                onValueChange(state.text.toString())
            }
            BasicTextField(
                modifier = Modifier.fillMaxWidth(),
                state = state,
                decorator = {
                    Box(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        it()
                    }
                },
            )
        },
    )
}

@Composable
private fun SettingItem(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.padding(
            horizontal = HorizontalPadding,
            vertical = 8.dp,
        ),
    ) {
        title()
        Spacer(modifier = Modifier.height(4.dp))
        content()
    }
}
