package net.matsudamper.gptclient.ui.chat

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "Chat processing")
@Composable
private fun ChatProcessingPreview() {
    MaterialTheme(
        colorScheme = lightColorScheme(),
    ) {
        Surface {
            LoadingMessageComposableInterface(
                onClickCancel = { },
            ).Content(modifier = Modifier)
        }
    }
}
