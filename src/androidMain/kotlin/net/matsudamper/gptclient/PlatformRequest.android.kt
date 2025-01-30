package net.matsudamper.gptclient

import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.channels.Channel

class AndroidPlatformRequest(activity: ComponentActivity) : PlatformRequest {
    private val mediaLauncher = object {
        private val resultFlow = Channel<List<String>>(Channel.RENDEZVOUS)
        private val launcher = activity.registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) {
            resultFlow.trySend(it.map { it.toString() })
        }

        suspend fun launch(): List<String> {
            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            return resultFlow.receive()
        }
    }

    override suspend fun getMedia(): List<String> {
        return mediaLauncher.launch()
    }
}
