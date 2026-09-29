package net.matsudamper.gptclient.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Cpu
import compose.icons.feathericons.CreditCard
import compose.icons.feathericons.Lock
import net.matsudamper.gptclient.ui.DISABLED_CONTENT_ALPHA

data class ModelSelectorUiState(
    val selectedModelName: String,
    val items: List<Item>,
    val thinkingEnabled: Boolean,
    val thinkingToggleEnabled: Boolean,
    val overflowMenu: OverflowMenu,
    val listener: Listener,
) {
    data class Item(
        val modelName: String,
        val selected: Boolean,
        val listener: ItemListener,
    )

    @Immutable
    interface ItemListener {
        fun onClick()
    }

    @Immutable
    interface Listener {
        fun onChangeThinking(enabled: Boolean)
    }

    @Immutable
    sealed interface OverflowMenu {
        data object None : OverflowMenu

        @Immutable
        data class Gemini(
            val billingKeyEnabled: Boolean,
            val billingKeyToggleEnabled: Boolean,
            val onChangeBillingKey: (Boolean) -> Unit,
        ) : OverflowMenu
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectorBar(
    uiState: ModelSelectorUiState,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExposedDropdownMenuBox(
            modifier = Modifier.weight(1f),
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedButton(
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                onClick = {},
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = uiState.selectedModelName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                )
            }

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                matchAnchorWidth = false,
            ) {
                for (model in uiState.items) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = model.modelName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        leadingIcon = {
                            if (model.selected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                )
                            } else {
                                Spacer(modifier = Modifier.width(24.dp))
                            }
                        },
                        onClick = {
                            expanded = false
                            model.listener.onClick()
                        },
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (val overflow = uiState.overflowMenu) {
                ModelSelectorUiState.OverflowMenu.None -> Unit
                is ModelSelectorUiState.OverflowMenu.Gemini -> {
                    ToggleIconButton(
                        imageVector = FeatherIcons.CreditCard,
                        label = "Billing",
                        checked = overflow.billingKeyEnabled,
                        enabled = overflow.billingKeyToggleEnabled,
                        onCheckedChange = overflow.onChangeBillingKey,
                    )
                }
            }
            ToggleIconButton(
                imageVector = FeatherIcons.Cpu,
                label = "Thinking",
                checked = uiState.thinkingEnabled,
                enabled = uiState.thinkingToggleEnabled,
                onCheckedChange = uiState.listener::onChangeThinking,
            )
        }
    }
}

@Composable
private fun ToggleIconButton(
    imageVector: ImageVector,
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { menuExpanded = true },
            enabled = enabled,
        ) {
            val stateColor = if (checked) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            Box {
                Icon(
                    imageVector = imageVector,
                    contentDescription = label,
                    tint = if (enabled) {
                        stateColor
                    } else {
                        stateColor.copy(alpha = DISABLED_CONTENT_ALPHA)
                    },
                )
                if (!enabled) {
                    Icon(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-6).dp)
                            .size(14.dp),
                        imageVector = FeatherIcons.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = label,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = checked,
                            enabled = enabled,
                            onCheckedChange = onCheckedChange,
                        )
                    }
                },
                onClick = { onCheckedChange(!checked) },
                enabled = enabled,
            )
        }
    }
}

@Composable
internal fun ModelSelectorBarPreviewContent(isDark: Boolean) {
    MaterialTheme(
        colorScheme = if (isDark) darkColorScheme() else lightColorScheme(),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column {
                ModelSelectorBar(
                    uiState = createPreviewUiState(
                        thinkingEnabled = true,
                        thinkingToggleEnabled = true,
                        billingKeyEnabled = true,
                        billingKeyToggleEnabled = true,
                    ),
                )
                ModelSelectorBar(
                    uiState = createPreviewUiState(
                        thinkingEnabled = false,
                        thinkingToggleEnabled = true,
                        billingKeyEnabled = false,
                        billingKeyToggleEnabled = true,
                    ),
                )
                ModelSelectorBar(
                    uiState = createPreviewUiState(
                        thinkingEnabled = false,
                        thinkingToggleEnabled = false,
                        billingKeyEnabled = true,
                        billingKeyToggleEnabled = false,
                    ),
                )
            }
        }
    }
}

private fun createPreviewUiState(
    thinkingEnabled: Boolean,
    thinkingToggleEnabled: Boolean,
    billingKeyEnabled: Boolean,
    billingKeyToggleEnabled: Boolean,
): ModelSelectorUiState {
    return ModelSelectorUiState(
        selectedModelName = "Gemini 3 Flash",
        items = listOf(),
        thinkingEnabled = thinkingEnabled,
        thinkingToggleEnabled = thinkingToggleEnabled,
        overflowMenu = ModelSelectorUiState.OverflowMenu.Gemini(
            billingKeyEnabled = billingKeyEnabled,
            billingKeyToggleEnabled = billingKeyToggleEnabled,
            onChangeBillingKey = {},
        ),
        listener = object : ModelSelectorUiState.Listener {
            override fun onChangeThinking(enabled: Boolean) = Unit
        },
    )
}
