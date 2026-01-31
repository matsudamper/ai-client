package net.matsudamper.gptclient.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

data class ChatErrorMessageRetryComposableInterface(
    private val message: AnnotatedString,
    private val retry: () -> Unit,
) : ChatMessageComposableInterface {
    @Composable
    override fun Content(modifier: Modifier) {
        Column(
            modifier = modifier,
        ) {
            SelectionContainer {
                Text(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(6.dp),
                    text = message,
                )
            }
            TextButton(
                onClick = retry,
                modifier = Modifier.padding(top = 8.dp)
                    .align(Alignment.End),
            ) {
                Text("Retry")
            }
        }
    }
}
