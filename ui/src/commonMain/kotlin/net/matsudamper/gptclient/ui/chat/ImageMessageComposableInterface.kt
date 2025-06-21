package net.matsudamper.gptclient.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

data class ImageMessageComposableInterface(
    val uiState: UiState,
) : ChatMessageComposableInterface {
    @Composable
    override fun Content(modifier: Modifier) {
        var showImageUri by remember { mutableStateOf<String?>(null) }
        if (showImageUri != null) {
            Dialog(
                onDismissRequest = { showImageUri = null },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                ),
            ) {
                AsyncImage(
                    modifier = Modifier.fillMaxSize()
                        .zoomable(rememberZoomState()),
                    model = showImageUri.orEmpty(),
                    contentScale = ContentScale.Fit,
                    contentDescription = null,
                )
            }
        }

        AsyncImage(
            modifier = modifier
                .size(200.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { showImageUri = uiState.url },
            contentScale = ContentScale.Crop,
            contentDescription = null,
            model = uiState.url,
        )
    }

    data class UiState(
        val url: String,
    )
}
