package net.matsudamper.gptclient.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
        val initialGeminiBillingKey: String,
        val themeOption: ThemeOption,
        val localModels: List<LocalModelItem>,
        val deleteDialog: DeleteDialog?,
        val listener: Listener,
    ) : SettingsScreenUiState {
        @Immutable
        interface Listener {
            fun updateSecretKey(text: String)
            fun updateGeminiSecretKey(text: String)
            fun updateGeminiBillingKey(text: String)
            fun onClickOpenAiUsage()
            fun onClickGeminiUsage()
            fun onClickThemeOption(themeOption: ThemeOption)
        }
    }

    data class DeleteDialog(
        val modelName: String,
        val listener: Listener,
    ) {
        @Immutable
        interface Listener {
            fun onConfirm()
            fun onDismiss()
        }
    }

    enum class ThemeOption {
        SYSTEM,
        LIGHT,
        DARK,
    }

    data class LocalModelItem(
        val modelId: String,
        val displayName: String,
        val description: String,
        val status: ModelStatus,
        val downloadProgress: Float?,
        val isActive: Boolean,
        val listener: Listener,
    ) {
        enum class ModelStatus {
            NOT_DOWNLOADED,
            DOWNLOADING,
            DOWNLOADED,
        }

        @Immutable
        interface Listener {
            fun onClickDownload()
            fun onToggleActive(active: Boolean)
            fun onClickDelete()
        }
    }
}

private val HorizontalPadding = 12.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
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
    uiState.deleteDialog?.let { dialog ->
        AlertDialog(
            onDismissRequest = { dialog.listener.onDismiss() },
            confirmButton = {
                TextButton(onClick = { dialog.listener.onConfirm() }) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { dialog.listener.onDismiss() }) {
                    Text("キャンセル")
                }
            },
            title = {
                Text("モデルを削除しますか？")
            },
            text = {
                Text("${dialog.modelName} を削除します。")
            },
        )
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
    ) {
        ThemeSettingItem(
            modifier = Modifier.fillMaxWidth(),
            currentThemeOption = uiState.themeOption,
            onClickThemeOption = { uiState.listener.onClickThemeOption(it) },
        )
        Spacer(modifier = Modifier.height(12.dp))
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
        Spacer(modifier = Modifier.height(12.dp))
        ApiKeySettingItem(
            modifier = Modifier.fillMaxWidth(),
            title = "Gemini Billing Key",
            initialValue = uiState.initialGeminiBillingKey,
            onValueChange = { uiState.listener.updateGeminiBillingKey(it) },
        )
        if (uiState.localModels.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            LocalModelSettingSection(
                modifier = Modifier.fillMaxWidth(),
                models = uiState.localModels,
            )
        }
    }
}

@Composable
private fun LocalModelSettingSection(
    modifier: Modifier = Modifier,
    models: List<SettingsScreenUiState.LocalModelItem>,
) {
    SettingItem(
        modifier = modifier,
        title = { Text("ローカルモデル") },
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                models.forEach { model ->
                    LocalModelCard(model = model)
                }
            }
        },
    )
}

@Composable
private fun LocalModelCard(
    model: SettingsScreenUiState.LocalModelItem,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
    ) {
        Text(
            text = model.displayName,
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = model.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))

        when (model.status) {
            SettingsScreenUiState.LocalModelItem.ModelStatus.NOT_DOWNLOADED -> {
                OutlinedButton(
                    onClick = { model.listener.onClickDownload() },
                ) {
                    Text("ダウンロード")
                }
            }

            SettingsScreenUiState.LocalModelItem.ModelStatus.DOWNLOADING -> {
                Text(
                    text = model.downloadProgress
                        ?.let { "ダウンロード中... ${(it * 100).toInt()}%" }
                        ?: "ダウンロード中...",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            SettingsScreenUiState.LocalModelItem.ModelStatus.DOWNLOADED -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "有効にする",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Switch(
                            checked = model.isActive,
                            onCheckedChange = { model.listener.onToggleActive(it) },
                        )
                        IconButton(
                            modifier = Modifier.size(40.dp),
                            onClick = { model.listener.onClickDelete() },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Model",
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSettingItem(
    modifier: Modifier = Modifier,
    currentThemeOption: SettingsScreenUiState.ThemeOption,
    onClickThemeOption: (SettingsScreenUiState.ThemeOption) -> Unit,
) {
    SettingItem(
        modifier = modifier,
        title = {
            Text("テーマ")
        },
        content = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SettingsScreenUiState.ThemeOption.entries.forEach { option ->
                    FilterChip(
                        selected = currentThemeOption == option,
                        onClick = { onClickThemeOption(option) },
                        label = {
                            Text(
                                when (option) {
                                    SettingsScreenUiState.ThemeOption.SYSTEM -> "端末に同期"
                                    SettingsScreenUiState.ThemeOption.LIGHT -> "ライト"
                                    SettingsScreenUiState.ThemeOption.DARK -> "ダーク"
                                },
                            )
                        },
                    )
                }
            }
        },
    )
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
