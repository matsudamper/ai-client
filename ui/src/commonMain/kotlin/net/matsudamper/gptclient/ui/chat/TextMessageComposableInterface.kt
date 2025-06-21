package net.matsudamper.gptclient.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

data class TextMessageComposableInterface(
    val uiState: AnnotatedString,
) : ChatMessageComposableInterface {
    @Composable
    override fun Content(modifier: Modifier) {
        SelectionContainer {
            Text(
                modifier = modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(6.dp),
                text = uiState,
            )
        }
    }
}
