package net.matsudamper.gptclient.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "Home")
@Composable
private fun HomePreview() {
    NewChatPreviewContent(isDark = false)
}

@Preview(name = "Home Dark")
@Composable
private fun HomeDarkPreview() {
    NewChatPreviewContent(isDark = true)
}
