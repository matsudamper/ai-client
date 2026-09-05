package net.matsudamper.gptclient.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ChevronRight
import net.matsudamper.gptclient.ui.platform.BackHandler

private enum class SettingsRoute {
    TOP,
    API_KEY,
    MODEL,
}

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
            fun onClickLatestRelease()
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
        val displayName: String,
        val description: String,
        val status: ModelStatus,
        val downloadProgress: Float?,
        val canDelete: Boolean,
        val isActive: Boolean,
        val listener: Listener,
    ) {
        enum class ModelStatus {
            UNAVAILABLE,
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
    var route by rememberSaveable { mutableStateOf(SettingsRoute.TOP) }
    BackHandler(enabled = route != SettingsRoute.TOP) {
        route = SettingsRoute.TOP
    }

    Scaffold(
        modifier = modifier.imePadding(),
        topBar = {
            SettingsTopBar(
                route = route,
                onClickMenu = onClickMenu,
                onClickBack = { route = SettingsRoute.TOP },
            )
        },
    ) { innerPadding ->
        when (uiState) {
            is SettingsScreenUiState.Loading -> {
                Loading(
                    modifier = Modifier.fillMaxSize()
                        .padding(innerPadding),
                )
            }

            is SettingsScreenUiState.Loaded -> {
                when (route) {
                    SettingsRoute.TOP -> {
                        SettingsTopContent(
                            uiState = uiState,
                            onClickApiKey = { route = SettingsRoute.API_KEY },
                            onClickModel = { route = SettingsRoute.MODEL },
                            modifier = Modifier.fillMaxSize()
                                .padding(innerPadding),
                        )
                    }

                    SettingsRoute.API_KEY -> {
                        SettingsApiKeyContent(
                            uiState = uiState,
                            modifier = Modifier.fillMaxSize()
                                .padding(innerPadding),
                        )
                    }

                    SettingsRoute.MODEL -> {
                        SettingsModelContent(
                            uiState = uiState,
                            modifier = Modifier.fillMaxSize()
                                .padding(innerPadding),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopBar(
    route: SettingsRoute,
    onClickMenu: () -> Unit,
    onClickBack: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                when (route) {
                    SettingsRoute.TOP -> "設定"
                    SettingsRoute.API_KEY -> "APIキー"
                    SettingsRoute.MODEL -> "モデル"
                },
            )
        },
        navigationIcon = {
            when (route) {
                SettingsRoute.TOP -> {
                    IconButton(onClick = onClickMenu) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                    }
                }

                SettingsRoute.API_KEY,
                SettingsRoute.MODEL,
                -> {
                    IconButton(onClick = onClickBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                }
            }
        },
    )
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
private fun SettingsTopContent(
    uiState: SettingsScreenUiState.Loaded,
    onClickApiKey: () -> Unit,
    onClickModel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberSaveable(saver = ScrollState.Saver) {
        ScrollState(initial = 0)
    }

    Column(
        modifier = modifier
            .padding(
                WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
            )
            .verticalScroll(scrollState),
    ) {
        ThemeSettingItem(
            modifier = Modifier.fillMaxWidth(),
            currentThemeOption = uiState.themeOption,
            onClickThemeOption = { uiState.listener.onClickThemeOption(it) },
        )
        SettingsNavigationItem(
            title = "APIキー",
            onClick = onClickApiKey,
        )
        SettingsNavigationItem(
            title = "モデル",
            onClick = onClickModel,
        )
        TextButton(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = HorizontalPadding),
            onClick = { uiState.listener.onClickLatestRelease() },
        ) {
            Text("GitHub リリース")
        }
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun SettingsApiKeyContent(
    uiState: SettingsScreenUiState.Loaded,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberSaveable(saver = ScrollState.Saver) {
        ScrollState(initial = 0)
    }

    Column(
        modifier = modifier
            .padding(
                WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
            )
            .verticalScroll(scrollState),
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
        Spacer(modifier = Modifier.height(12.dp))
        ApiKeySettingItem(
            modifier = Modifier.fillMaxWidth(),
            title = "Gemini Billing Key",
            initialValue = uiState.initialGeminiBillingKey,
            onValueChange = { uiState.listener.updateGeminiBillingKey(it) },
        )
        Spacer(modifier = Modifier.height(12.dp))
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun SettingsModelContent(
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

    val scrollState = rememberSaveable(saver = ScrollState.Saver) {
        ScrollState(initial = 0)
    }

    Column(
        modifier = modifier
            .padding(
                WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
            )
            .verticalScroll(scrollState),
    ) {
        if (uiState.localModels.isEmpty()) {
            Text(
                modifier = Modifier.padding(
                    horizontal = HorizontalPadding,
                    vertical = 16.dp,
                ),
                text = "利用可能なローカルモデルがありません",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LocalModelSettingSection(
                modifier = Modifier.fillMaxWidth(),
                models = uiState.localModels,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun SettingsNavigationItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = HorizontalPadding,
                vertical = 16.dp,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
        )
        Icon(
            imageVector = FeatherIcons.ChevronRight,
            contentDescription = null,
        )
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
            SettingsScreenUiState.LocalModelItem.ModelStatus.UNAVAILABLE -> {
                Text(
                    text = "このデバイスでは利用できません",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

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
                        if (model.canDelete) {
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
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
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
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
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

@Composable
internal fun SettingsScreenPreviewContent(
    isDark: Boolean,
) {
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
        Surface(
            color = MaterialTheme.colorScheme.surface,
        ) {
            SettingsScreen(
                uiState = SettingsScreenUiState.Loaded(
                    initialSecretKey = "sk-test-openai-key",
                    initialGeminiSecretKey = "AIza-test-gemini-key",
                    initialGeminiBillingKey = "billing-test-key",
                    themeOption = SettingsScreenUiState.ThemeOption.DARK,
                    localModels = listOf(),
                    deleteDialog = null,
                    listener = object : SettingsScreenUiState.Loaded.Listener {
                        override fun updateSecretKey(text: String) = Unit

                        override fun updateGeminiSecretKey(text: String) = Unit

                        override fun updateGeminiBillingKey(text: String) = Unit

                        override fun onClickOpenAiUsage() = Unit

                        override fun onClickGeminiUsage() = Unit

                        override fun onClickLatestRelease() = Unit

                        override fun onClickThemeOption(themeOption: SettingsScreenUiState.ThemeOption) = Unit
                    },
                ),
                onClickMenu = {},
            )
        }
    }
}

@Composable
internal fun SettingsApiKeyScreenPreviewContent(
    isDark: Boolean,
) {
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
        Surface(
            color = MaterialTheme.colorScheme.surface,
        ) {
            SettingsApiKeyContent(
                uiState = SettingsScreenUiState.Loaded(
                    initialSecretKey = "sk-test-openai-key",
                    initialGeminiSecretKey = "AIza-test-gemini-key",
                    initialGeminiBillingKey = "billing-test-key",
                    themeOption = SettingsScreenUiState.ThemeOption.DARK,
                    localModels = listOf(),
                    deleteDialog = null,
                    listener = object : SettingsScreenUiState.Loaded.Listener {
                        override fun updateSecretKey(text: String) = Unit

                        override fun updateGeminiSecretKey(text: String) = Unit

                        override fun updateGeminiBillingKey(text: String) = Unit

                        override fun onClickOpenAiUsage() = Unit

                        override fun onClickGeminiUsage() = Unit

                        override fun onClickLatestRelease() = Unit

                        override fun onClickThemeOption(themeOption: SettingsScreenUiState.ThemeOption) = Unit
                    },
                ),
            )
        }
    }
}
