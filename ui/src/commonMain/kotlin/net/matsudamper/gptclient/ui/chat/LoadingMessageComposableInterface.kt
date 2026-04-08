package net.matsudamper.gptclient.ui.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data object LoadingMessageComposableInterface : ChatMessageComposableInterface {
    private val uiState: Unit = Unit

    @Composable
    override fun Content(modifier: Modifier) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Box(
                modifier = Modifier.padding(6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "       ",
                )
                LinearProgressIndicator()
            }
        }
    }
}
