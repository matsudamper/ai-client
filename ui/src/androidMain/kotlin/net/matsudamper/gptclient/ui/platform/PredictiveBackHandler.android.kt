package net.matsudamper.gptclient.ui.platform

import androidx.compose.runtime.Composable
import kotlin.coroutines.cancellation.CancellationException

@Composable
actual fun PredictiveBackHandler(
    enabled: Boolean,
    onProgress: (Float) -> Unit,
    onBackInvoked: () -> Unit,
    onBackCancelled: () -> Unit,
) {
    androidx.activity.compose.PredictiveBackHandler(enabled) { progress ->
        try {
            progress.collect { event ->
                onProgress(event.progress)
            }
            onBackInvoked()
        } catch (e: CancellationException) {
            onBackCancelled()
            throw e
        }
    }
}
