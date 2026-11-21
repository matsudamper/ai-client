package net.matsudamper.gptclient.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "Chat list")
@Composable
private fun ChatListPreview() {
    ChatListPreviewContent(isDark = false)
}

@Preview(name = "Chat list dark")
@Composable
private fun ChatListDarkPreview() {
    ChatListPreviewContent(isDark = true)
}
