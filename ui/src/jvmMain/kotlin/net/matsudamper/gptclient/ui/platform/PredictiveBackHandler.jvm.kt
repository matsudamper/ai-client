package net.matsudamper.gptclient.ui.platform

import androidx.compose.runtime.Composable

@Composable
actual fun PredictiveBackHandler(
    enabled: Boolean,
    onProgress: (Float) -> Unit,
    onBackInvoked: () -> Unit,
    onBackCancelled: () -> Unit,
) {
}
