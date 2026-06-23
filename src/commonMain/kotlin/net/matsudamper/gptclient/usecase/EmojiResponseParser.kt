package net.matsudamper.gptclient.usecase

import kotlinx.serialization.json.Json
import net.matsudamper.gptclient.ui.jsonui.UiNode
import net.matsudamper.gptclient.util.Log
import net.matsudamper.gptclient.viewmodel.EmojiGptResponse

class EmojiResponseParser {
    @Suppress("PrivatePropertyName")
    private val Json = Json { ignoreUnknownKeys = true }

    fun toUiNode(original: String): UiNode = try {
        val response = Json
            .decodeFromString<EmojiGptResponse>(original)
        Log.d("RESPONSE", response.toString())
        if (response.results.isEmpty()) {
            UiNode.Txt(v = response.errorMessage ?: original)
        } else {
            UiNode.Chips(v = response.results)
        }
    } catch (e: Throwable) {
        e.printStackTrace()
        UiNode.Txt(v = original)
    }
}
