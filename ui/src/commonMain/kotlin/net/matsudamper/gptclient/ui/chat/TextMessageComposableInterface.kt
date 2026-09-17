package net.matsudamper.gptclient.ui.chat

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

data class TextMessageComposableInterface(
    val uiState: AnnotatedString,
) : ChatMessageComposableInterface {
    @Composable
    override fun Content(containerColor: Color, modifier: Modifier) {
        SelectionContainer {
            Surface(
                modifier = modifier,
                shape = MaterialTheme.shapes.small,
                color = containerColor,
            ) {
                Text(
                    modifier = Modifier.padding(6.dp),
                    text = uiState,
                )
            }
        }
    }
}
