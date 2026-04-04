package net.matsudamper.gptclient.util

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.receiveAsFlow

public class EventHandler<Receiver>(
    private val events: ReceiveChannel<suspend (Receiver) -> Unit>,
) {
    public suspend fun collect(target: Receiver) {
        events.receiveAsFlow().collect { block ->
            block(target)
        }
    }
}
