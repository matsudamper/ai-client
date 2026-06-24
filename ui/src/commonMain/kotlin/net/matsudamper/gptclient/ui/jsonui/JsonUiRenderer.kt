package net.matsudamper.gptclient.ui.jsonui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JsonUiRenderer(
    node: UiNode,
    modifier: Modifier = Modifier,
    onChipClick: ((String) -> Unit)? = null,
) {
    when (node) {
        is UiNode.Column -> {
            Column(
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (child in node.children) {
                    JsonUiRenderer(node = child, onChipClick = onChipClick)
                }
            }
        }

        is UiNode.Row -> {
            FlowRow(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (child in node.children) {
                    JsonUiRenderer(node = child, onChipClick = onChipClick)
                }
            }
        }

        is UiNode.Text -> {
            val textStyle = when (node.style) {
                "h" -> MaterialTheme.typography.titleMedium
                "b" -> MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                "sm" -> MaterialTheme.typography.bodySmall
                else -> MaterialTheme.typography.bodyMedium
            }
            Text(
                modifier = modifier,
                text = node.value,
                style = textStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        is UiNode.Link -> {
            val uriHandler = LocalUriHandler.current
            TextButton(
                modifier = modifier,
                onClick = { uriHandler.openUri(node.url) },
                contentPadding = PaddingValues(horizontal = 0.dp),
            ) {
                Text(text = node.label)
            }
        }

        is UiNode.KeyValue -> {
            Row(modifier = modifier) {
                Text(
                    text = "${node.key}: ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = node.value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        is UiNode.Chips -> {
            FlowRow(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (chip in node.values) {
                    OutlinedButton(
                        onClick = { onChipClick?.invoke(chip) },
                    ) {
                        Text(
                            text = chip,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                }
            }
        }

        is UiNode.Divider -> {
            HorizontalDivider(modifier = modifier.padding(vertical = 4.dp))
        }
    }
}
