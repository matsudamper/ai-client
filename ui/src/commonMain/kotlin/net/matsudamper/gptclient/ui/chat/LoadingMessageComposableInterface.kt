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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class LoadingMessageComposableInterface(
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
                TextButton(onClick = onClickCancel) {
                    Text("キャンセル")
                }
            }
        }
    }
}
