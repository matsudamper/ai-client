package net.matsudamper.gptclient.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowUp
import compose.icons.feathericons.Image
import compose.icons.feathericons.Mic
import compose.icons.feathericons.RotateCcw
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

@Composable
internal fun ChatFooter(
    textFieldState: TextFieldState,
    selectedMedia: List<String>,
    visibleMediaLoading: Boolean,
    onClickImage: () -> Unit,
    onClickVoice: () -> Unit,
    onClickSend: () -> Unit,
    onClickRetry: (() -> Unit)?,
    onImageCrop: (imageUri: String, cropRect: Rect, imageSize: IntSize) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier,
) {
    var showImageUri by remember { mutableStateOf<String?>(null) }
    val showCropImageUriState = remember { mutableStateOf<String?>(null) }

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

    val showCropImageUri = showCropImageUriState.value
    if (showCropImageUri != null) {
        ImageCropDialog(
            imageUri = showCropImageUri,
            onDismissRequest = { showCropImageUriState.value = null },
            onCropComplete = { cropRect, imageSize ->
                onImageCrop(showCropImageUri, cropRect, imageSize)
                showCropImageUriState.value = null
            },
        )
    }
    Column(
        modifier = modifier,
    ) {
        val imageModifier = Modifier.size(120.dp)
            .padding(12.dp)
        LazyRow {
            items(selectedMedia) { media ->
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    AsyncImage(
                        modifier = Modifier.clickable {
                            showMenu = true
                        }.then(imageModifier),
                        model = media,
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                    )

                    androidx.compose.material3.DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { androidx.compose.material3.Text("View") },
                            onClick = {
                                showImageUri = media
                                showMenu = false
                            },
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { androidx.compose.material3.Text("Crop") },
                            onClick = {
                                showCropImageUriState.value = media
                                showMenu = false
                            },
                        )
                    }
                }
            }
            if (visibleMediaLoading) {
                item {
                    Box(
                        modifier = imageModifier
                            .background(MaterialTheme.colorScheme.secondary),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onSecondary,
                        )
                    }
                }
            }
        }
        FooterTextSection(
            modifier = Modifier.fillMaxWidth(),
            onClickSelectImage = onClickImage,
            textFieldState = textFieldState,
            onClickVoice = onClickVoice,
            onClickSend = onClickSend,
            onClickRetry = onClickRetry,
        )
    }
}

@Composable
private fun FooterTextSection(
    textFieldState: TextFieldState,
    onClickSelectImage: () -> Unit,
    onClickVoice: () -> Unit,
    onClickSend: () -> Unit,
    onClickRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onClickSelectImage() }) {
            Icon(
                imageVector = FeatherIcons.Image,
                contentDescription = "add image",
            )
        }
        Row(
            modifier = Modifier.weight(1f)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.onSecondary)
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                modifier = modifier
                    .fillMaxHeight()
                    .weight(1f),
                state = textFieldState,
                decorator = {
                    Box(contentAlignment = Alignment.CenterStart) {
                        it()
                    }
                },
            )
            IconButton(
                modifier = Modifier.size(32.dp),
                onClick = { onClickVoice() },
            ) {
                Icon(
                    modifier = Modifier.padding(4.dp),
                    imageVector = FeatherIcons.Mic,
                    contentDescription = "input voice",
                )
            }
        }

        if (onClickRetry != null) {
            IconButton(onClick = { onClickRetry() }) {
                Icon(
                    imageVector = FeatherIcons.RotateCcw,
                    contentDescription = "retry",
                )
            }
        }

        IconButton(onClick = { onClickSend() }) {
            Icon(
                imageVector = FeatherIcons.ArrowUp,
                contentDescription = "send",
            )
        }
    }
}
