package net.matsudamper.gptclient.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowUp
import compose.icons.feathericons.Image
import compose.icons.feathericons.Mic
import compose.icons.feathericons.RotateCcw
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

data class ChatFooterImage(
    val imageUri: String,
    val rect: Rect?,
    val listener: Listener,
) {
    data class Rect(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
    )

    @Immutable
    interface Listener {
        fun crop(rect: Rect)
        fun delete()
    }
}

@Composable
internal fun ChatFooter(
    textFieldState: TextFieldState,
    selectedMedia: List<ChatFooterImage>,
    visibleMediaLoading: Boolean,
    enableSend: Boolean,
    onClickAddImage: () -> Unit,
    onClickVoice: () -> Unit,
    onClickSend: () -> Unit,
    onClickRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val showImageState = remember { mutableStateOf<ChatFooterImage?>(null) }
    val showCropImageState = remember { mutableStateOf<ChatFooterImage?>(null) }

    val showImage = showImageState.value
    if (showImage != null) {
        Dialog(
            onDismissRequest = { showImageState.value = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
            ),
        ) {
            CroppedImageView(
                imageUri = showImage.imageUri,
                cropRect = showImage.rect,
                modifier = Modifier.fillMaxSize()
                    .zoomable(rememberZoomState()),
            )
        }
    }

    val showCropImageUri = showCropImageState.value
    if (showCropImageUri != null) {
        ImageCropDialog(
            imageUri = showCropImageUri.imageUri,
            initialRect = remember(showCropImageUri.rect) {
                if (showCropImageUri.rect == null) return@remember null
                Rect(
                    left = showCropImageUri.rect.left,
                    top = showCropImageUri.rect.top,
                    right = showCropImageUri.rect.right,
                    bottom = showCropImageUri.rect.bottom,
                )
            },
            onDismissRequest = { showCropImageState.value = null },
            onCropComplete = { cropRect ->
                showCropImageUri.listener.crop(
                    ChatFooterImage.Rect(
                        left = cropRect.left,
                        top = cropRect.top,
                        right = cropRect.right,
                        bottom = cropRect.bottom,
                    ),
                )
                showCropImageState.value = null
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
                        modifier = Modifier
                            .combinedClickable(
                                onLongClick = { showMenu = true },
                                onClick = { showMenu = true },
                            )
                            .then(imageModifier),
                        model = media.imageUri,
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
                                showImageState.value = media
                                showMenu = false
                            },
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { androidx.compose.material3.Text("Crop") },
                            onClick = {
                                showCropImageState.value = media
                                showMenu = false
                            },
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { androidx.compose.material3.Text("Delete") },
                            onClick = {
                                media.listener.delete()
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
            onClickSelectImage = onClickAddImage,
            textFieldState = textFieldState,
            onClickVoice = onClickVoice,
            onClickSend = onClickSend,
            onClickRetry = onClickRetry,
            enableSend = enableSend,
        )
    }
}

@Composable
private fun FooterTextSection(
    textFieldState: TextFieldState,
    enableSend: Boolean,
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

        IconButton(
            onClick = { onClickSend() },
            enabled = enableSend,
        ) {
            Icon(
                imageVector = FeatherIcons.ArrowUp,
                contentDescription = "send",
            )
        }
    }
}

@Composable
private fun CroppedImageView(
    imageUri: String,
    cropRect: ChatFooterImage.Rect?,
    modifier: Modifier = Modifier,
) {
    val asyncImagePainter = rememberAsyncImagePainter(imageUri)
    val asyncImageState by asyncImagePainter.state.collectAsState()

    when (val state = asyncImageState) {
        is AsyncImagePainter.State.Success -> {
            BoxWithConstraints(
                modifier = modifier
                    .fillMaxSize()
                    .clipToBounds(),
            ) {
                val maxWidth = this.maxWidth
                val maxHeight = this.maxHeight
                val containerSize = with(LocalDensity.current) {
                    Size(maxWidth.toPx(), maxHeight.toPx())
                }

                Canvas(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val imageLeft = cropRect?.left ?: 0f
                    val imageTop = cropRect?.top ?: 0f
                    val imageRight = cropRect?.right ?: state.painter.intrinsicSize.width
                    val imageBottom = cropRect?.bottom ?: state.painter.intrinsicSize.height

                    val imageWidth = imageRight - imageLeft
                    val imageHeight = imageBottom - imageTop

                    val widthScale = containerSize.width / imageWidth
                    val heightScale = containerSize.height / imageHeight
                    val scale = if (widthScale > heightScale) {
                        heightScale
                    } else {
                        widthScale
                    }

                    val imageSize = Size(imageWidth, imageHeight) * scale
                    val offset = IntOffset(
                        x = ((containerSize.width - imageSize.width) / 2).toInt(),
                        y = ((containerSize.height - imageSize.height) / 2).toInt(),
                    )

                    translate(left = offset.x.toFloat(), top = offset.y.toFloat()) {
                        scale(
                            scale = scale,
                            pivot = Offset.Zero,
                        ) {
                            translate(left = -imageLeft, top = -imageTop) {
                                clipRect(
                                    left = imageLeft,
                                    top = imageTop,
                                    right = imageRight,
                                    bottom = imageBottom,
                                ) {
                                    with(state.painter) {
                                        draw(
                                            size = state.painter.intrinsicSize,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        is AsyncImagePainter.State.Loading -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        else -> {
            AsyncImage(
                modifier = modifier,
                model = imageUri,
                contentScale = ContentScale.Fit,
                contentDescription = null,
            )
        }
    }
}
