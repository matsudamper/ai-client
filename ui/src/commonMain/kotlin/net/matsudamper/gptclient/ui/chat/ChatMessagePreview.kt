package net.matsudamper.gptclient.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
private fun ChatMessagesPreviewContent(isDark: Boolean) {
    MaterialTheme(
        colorScheme = if (isDark) darkColorScheme() else lightColorScheme(),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextMessageComposableInterface(
                    uiState = AnnotatedString("ダークテーマの色を確認したい"),
                ).Content(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier,
                )
                TextMessageComposableInterface(
                    uiState = AnnotatedString("送信者ごとにコンテナ色を変えています"),
                ).Content(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier,
                )
                ChatErrorMessageRetryComposableInterface(
                    message = AnnotatedString("リクエストに失敗しました"),
                    retry = { },
                ).Content(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier,
                )
            }
        }
    }
}

@Preview(name = "Chat messages")
@Composable
private fun ChatMessagesPreview() {
    ChatMessagesPreviewContent(isDark = false)
}

@Preview(name = "Chat messages dark")
@Composable
private fun ChatMessagesDarkPreview() {
    ChatMessagesPreviewContent(isDark = true)
}
