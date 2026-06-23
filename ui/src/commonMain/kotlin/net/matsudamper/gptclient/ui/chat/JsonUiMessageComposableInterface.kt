package net.matsudamper.gptclient.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

/**
 * [JsonUiNode] のツリーをComposeで描画するメッセージ。
 * ボタンの押下時はコピー([onCopy])やURLオープンを行う。
 */
data class JsonUiMessageComposableInterface(
    val root: JsonUiNode,
    val onCopy: (String) -> Unit,
) : ChatMessageComposableInterface {
    @Composable
    override fun Content(modifier: Modifier) {
        Box(
            modifier = modifier
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(8.dp),
        ) {
            NodeContent(root)
        }
    }

    @Composable
    private fun NodeContent(node: JsonUiNode) {
        when (node) {
            is JsonUiNode.Column -> {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    node.children.forEach { NodeContent(it) }
                }
            }

            is JsonUiNode.Row -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    node.children.forEach { NodeContent(it) }
                }
            }

            is JsonUiNode.Text -> {
                Text(
                    text = node.value,
                    style = when (node.style) {
                        "h" -> MaterialTheme.typography.titleMedium
                        "l" -> MaterialTheme.typography.labelSmall
                        else -> MaterialTheme.typography.bodyMedium
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            is JsonUiNode.Button -> {
                val uriHandler = LocalUriHandler.current
                OutlinedButton(
                    onClick = {
                        node.copyText?.let { onCopy(it) }
                        openAllowedUri(uriHandler, node.url)
                    },
                ) {
                    Text(node.value)
                }
            }

            is JsonUiNode.Link -> {
                val uriHandler = LocalUriHandler.current
                Text(
                    modifier = Modifier.clickable { openAllowedUri(uriHandler, node.url) },
                    text = node.value,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                )
            }

            JsonUiNode.Divider -> {
                HorizontalDivider(modifier = Modifier.fillMaxWidth())
            }
        }
    }

    private fun openAllowedUri(uriHandler: UriHandler, rawUrl: String?) {
        val url = rawUrl?.trim() ?: return
        if (!url.startsWith("https://") && !url.startsWith("http://")) return
        runCatching { uriHandler.openUri(url) }
    }
}
