package net.matsudamper.gptclient.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

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
            Text(
                text = "Thinking",
                color = if (uiState.thinkingToggleEnabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = uiState.thinkingEnabled,
                enabled = uiState.thinkingToggleEnabled,
                onCheckedChange = uiState.listener::onChangeThinking,
            )
            when (val overflow = uiState.overflowMenu) {
                ModelSelectorUiState.OverflowMenu.None -> Unit
                is ModelSelectorUiState.OverflowMenu.Gemini -> {
                    GeminiOverflowMenu(state = overflow)
                }
            }
        }
    }
}

@Composable
private fun GeminiOverflowMenu(
    state: ModelSelectorUiState.OverflowMenu.Gemini,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    IconButton(onClick = { menuExpanded = true }) {
        Icon(
            imageVector = Icons.Default.Menu,
            contentDescription = null,
        )
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
                        text = "Billing",
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = state.billingKeyEnabled,
                        enabled = state.billingKeyToggleEnabled,
                        onCheckedChange = state.onChangeBillingKey,
                    )
                }
            },
            onClick = {},
        )
    }
}
