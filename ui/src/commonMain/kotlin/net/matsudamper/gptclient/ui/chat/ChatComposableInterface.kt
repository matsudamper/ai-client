package net.matsudamper.gptclient.ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier

@Immutable
interface ChatMessageComposableInterface {
    @Composable
    fun Content(modifier: Modifier = Modifier)
}
