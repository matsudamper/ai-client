package net.matsudamper.gptclient.ui.chat

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.matsudamper.gptclient.ui.jsonui.JsonUiRenderer
import net.matsudamper.gptclient.ui.jsonui.UiNode

data class JsonUiMessageComposableInterface(
    val node: UiNode,
    val onChipClick: ((String) -> Unit)? = null,
) : ChatMessageComposableInterface {
    @Composable
    override fun Content(containerColor: Color, modifier: Modifier) {
        SelectionContainer {
            Surface(
                modifier = modifier,
                shape = MaterialTheme.shapes.small,
                color = containerColor,
            ) {
                JsonUiRenderer(
                    node = node,
                    modifier = Modifier.padding(6.dp),
                    onChipClick = onChipClick,
                )
            }
        }
    }
}
