package net.matsudamper.gptclient.ui.chat

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.delay
import net.matsudamper.gptclient.ui.util.formatElapsedDuration

class LoadingMessageComposableInterface(
    private val processingStartedAt: Instant?,
    private val onClickCancel: () -> Unit,
) : ChatMessageComposableInterface {
    @Composable
    override fun Content(containerColor: Color, modifier: Modifier) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.small,
            color = containerColor,
        ) {
            Row(
                modifier = Modifier
                    .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.width(72.dp),
                )
                if (processingStartedAt != null) {
                    val elapsedText by produceState(
                        initialValue = formatElapsedDuration(Duration.between(processingStartedAt, Instant.now())),
                        key1 = processingStartedAt,
                    ) {
                        while (true) {
                            value = formatElapsedDuration(Duration.between(processingStartedAt, Instant.now()))
                            delay(1_000)
                        }
                    }
                    Text(
                        text = elapsedText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                TextButton(onClick = onClickCancel) {
                    Text("キャンセル")
                }
            }
        }
    }
}
