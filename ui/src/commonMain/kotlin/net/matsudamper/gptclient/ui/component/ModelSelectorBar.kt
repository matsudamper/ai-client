package net.matsudamper.gptclient.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
                        imageVector = BillingIcon,
                        label = "Billing",
                        checked = overflow.billingKeyEnabled,
                        enabled = overflow.billingKeyToggleEnabled,
                        onCheckedChange = overflow.onChangeBillingKey,
                    )
                }
            }
            ToggleIconButton(
                imageVector = ThinkingIcon,
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
            Icon(
                imageVector = imageVector,
                contentDescription = label,
                tint = when {
                    !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTENT_ALPHA)
                    checked -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
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

private val ThinkingIcon: ImageVector = ImageVector.Builder(
    name = "Thinking",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).addPath(
    pathData = addPathNodes(
        "M9,21c0,0.55 0.45,1 1,1h4c0.55,0 1,-0.45 1,-1v-1H9v1z" +
            "M12,2C8.14,2 5,5.14 5,9c0,2.38 1.19,4.47 3,5.74V17c0,0.55 0.45,1 1,1h6c0.55,0 1,-0.45 1,-1v-2.26" +
            "c1.81,-1.27 3,-3.36 3,-5.74 0,-3.86 -3.14,-7 -7,-7z",
    ),
    fill = SolidColor(Color.Black),
).build()

private val BillingIcon: ImageVector = ImageVector.Builder(
    name = "Billing",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).addPath(
    pathData = addPathNodes(
        "M20,4H4c-1.11,0 -1.99,0.89 -1.99,2L2,18c0,1.11 0.89,2 2,2h16c1.11,0 2,-0.89 2,-2V6" +
            "c0,-1.11 -0.89,-2 -2,-2zM20,18H4v-6h16v6zM20,8H4V6h16v2z",
    ),
    fill = SolidColor(Color.Black),
).build()
