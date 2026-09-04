package net.matsudamper.gptclient.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "Settings Light")
@Composable
private fun SettingsScreenLightPreview() {
    SettingsScreenPreviewContent(isDark = false)
}

@Preview(name = "Settings Dark")
@Composable
private fun SettingsScreenDarkPreview() {
    SettingsScreenPreviewContent(isDark = true)
}

@Preview(name = "Settings ApiKey Light")
@Composable
private fun SettingsApiKeyScreenLightPreview() {
    SettingsApiKeyScreenPreviewContent(isDark = false)
}

@Preview(name = "Settings ApiKey Dark")
@Composable
private fun SettingsApiKeyScreenDarkPreview() {
    SettingsApiKeyScreenPreviewContent(isDark = true)
}
