package net.matsudamper.gptclient.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "Model selector bar")
@Composable
private fun ModelSelectorBarPreview() {
    ModelSelectorBarPreviewContent(isDark = false)
}

@Preview(name = "Model selector bar dark")
@Composable
private fun ModelSelectorBarDarkPreview() {
    ModelSelectorBarPreviewContent(isDark = true)
}
