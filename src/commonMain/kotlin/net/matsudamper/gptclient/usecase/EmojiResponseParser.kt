package net.matsudamper.gptclient.usecase

import androidx.compose.ui.text.AnnotatedString
import kotlinx.serialization.json.Json
import net.matsudamper.gptclient.ui.chat.ChatMessageComposableInterface
import net.matsudamper.gptclient.ui.chat.EmojiMessageComposableInterface
import net.matsudamper.gptclient.ui.chat.TextMessageComposableInterface
import net.matsudamper.gptclient.viewmodel.EmojiGptResponse

class EmojiResponseParser {
    @Suppress("PrivatePropertyName")
    private val Json = Json { ignoreUnknownKeys = true }

    fun getEmojiList(original: String, onClick: (String) -> Unit): ChatMessageComposableInterface = try {
        val response = Json
            .decodeFromString<EmojiGptResponse>(original)
        println(response)
        if (response.results.isEmpty()) {
            TextMessageComposableInterface(
                AnnotatedString(response.errorMessage ?: original),
            )
        } else {
            EmojiMessageComposableInterface(
                EmojiMessageComposableInterface.UiState(
                    response.results.map {
                        EmojiMessageComposableInterface.UiState.Emoji(
                            value = it,
                            onClick = { onClick(it) },
                        )
                    },
                ),
            )
        }
    } catch (e: Throwable) {
        e.printStackTrace()
        TextMessageComposableInterface(AnnotatedString(original))
    }
}
