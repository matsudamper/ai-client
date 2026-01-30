package net.matsudamper.gptclient

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import kotlin.time.Duration
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

suspend fun SemanticsNodeInteractionsProvider.waitUntilExactlyOne(
    matcher: SemanticsMatcher,
    timeout: Duration,
    useUnmergedTree: Boolean = false,
): SemanticsNodeInteraction {
    var lastError: Throwable? = null
    var result: SemanticsNodeInteraction? = null
    runCatching {
        withTimeout(timeout) {
            while (true) {
                try {
                    val nodeInteraction = onNode(
                        matcher = matcher,
                        useUnmergedTree = useUnmergedTree,
                    )
                        .assertExists()

                    result = nodeInteraction
                    break
                } catch (e: Throwable) {
                    lastError = e
                }
                delay(50)
            }
        }
    }.onFailure { e ->
        throw e.initCause(lastError)
    }

    return result!!
}
