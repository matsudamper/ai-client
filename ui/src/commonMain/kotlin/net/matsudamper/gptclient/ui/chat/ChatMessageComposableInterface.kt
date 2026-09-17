package net.matsudamper.gptclient.ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Immutable
interface ChatMessageComposableInterface {
    @Composable
    fun Content(containerColor: Color, modifier: Modifier = Modifier)
}
