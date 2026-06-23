package net.matsudamper.gptclient.ui.chat

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(name = "JsonUiMessage")
@Composable
private fun JsonUiMessagePreview() {
    val json = """
        {"t":"col","c":[
          {"t":"txt","v":"打ち合わせ","s":"h"},
          {"t":"txt","v":"2026/06/23 15:00〜16:00"},
          {"t":"txt","v":"会議室A","s":"l"},
          {"t":"hr"},
          {"t":"row","c":[
            {"t":"btn","v":"コピー","cp":"打ち合わせ"},
            {"t":"lnk","v":"カレンダーに追加","u":"https://example.com"}
          ]}
        ]}
    """.trimIndent()
    val node = JsonUiParser.parseOrNull(json) ?: JsonUiNode.Text("parse error")
    MaterialTheme(colorScheme = lightColorScheme()) {
        Surface {
            CompositionLocalProvider(
                LocalUriHandler provides object : UriHandler {
                    override fun openUri(uri: String) = Unit
                },
            ) {
                JsonUiMessageComposableInterface(
                    root = node,
                    onCopy = {},
                ).Content(modifier = Modifier.padding(16.dp))
            }
        }
    }
}
