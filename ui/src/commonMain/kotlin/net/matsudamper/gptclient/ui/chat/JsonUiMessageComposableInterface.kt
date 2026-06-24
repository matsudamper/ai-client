package net.matsudamper.gptclient.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import net.matsudamper.gptclient.ui.jsonui.JsonUiRenderer
import net.matsudamper.gptclient.ui.jsonui.UiNode

data class JsonUiMessageComposableInterface(
    val node: UiNode,
    val onChipClick: ((String) -> Unit)? = null,
) : ChatMessageComposableInterface {
    @Composable
    override fun Content(modifier: Modifier) {
        SelectionContainer {
            JsonUiRenderer(
                node = node,
                modifier = modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(6.dp),
                onChipClick = onChipClick,
            )
        }
    }
}
