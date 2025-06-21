package net.matsudamper.gptclient.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class EmojiMessageComposableInterface(
    val uiState: UiState,
) : ChatMessageComposableInterface {
    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun Content(modifier: Modifier) {
        FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (emoji in uiState.emojiList) {
                OutlinedButton(
                    onClick = { emoji.onClick() },
                ) {
                    Text(
                        text = emoji.value,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
        }
    }

    data class UiState(
        val emojiList: List<Emoji>,
    ) {
        data class Emoji(
            val value: String,
            val onClick: () -> Unit,
        )
    }
}
