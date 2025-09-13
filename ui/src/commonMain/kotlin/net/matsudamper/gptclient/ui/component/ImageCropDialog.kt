package net.matsudamper.gptclient.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import net.matsudamper.gptclient.ui.systemGestureExclusion

private const val EDGE_DETECTION_THRESHOLD = 40f
private const val INITIAL_CROP_SIZE_RATIO = 0.8f

@Composable
fun ImageCropDialog(
    imageUri: String,
    initialRect: Rect?,
    onDismissRequest: () -> Unit,
    onCropComplete: (cropRect: Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageRectProviderFlow = remember {
        Channel<(Rect?) -> Unit>(Channel.UNLIMITED)
    }
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
        ),
    ) {
        Column(
            modifier = modifier,
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black)
                    .padding(24.dp),
            ) {
                ImageContent(
                    imageUri = imageUri,
                    initialRect = initialRect,
                    imageRectProviderFlow = remember(imageRectProviderFlow) { imageRectProviderFlow.consumeAsFlow() },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier.padding(end = 8.dp),
                ) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.weight(1f))

                val coroutineScope = rememberCoroutineScope()
                Button(
                    onClick = {
                        coroutineScope.launch {
                            imageRectProviderFlow.send { rect ->
                                if (rect != null) {
                                    onCropComplete(rect)
                                }
                            }
                        }
                    },
                ) {
                    Text("Complete")
                }
            }

        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImageContent(
    imageUri: String,
    initialRect: Rect?,
    imageRectProviderFlow: Flow<(Rect?) -> Unit>,
    modifier: Modifier = Modifier,
) {
    val asyncImage = rememberAsyncImagePainter(imageUri)
    val asyncImageState by asyncImage.state.collectAsStateWithLifecycle()
    when (val asyncImageState = asyncImageState) {
        is AsyncImagePainter.State.Empty,
        is AsyncImagePainter.State.Error,
        is AsyncImagePainter.State.Loading,
            -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        is AsyncImagePainter.State.Success -> {
            BoxWithConstraints(
                modifier = modifier.clipToBounds().systemGestureExclusion(),
            ) {
                val maxWidth = this.maxWidth
                val maxHeight = this.maxHeight
                val containerSize = with(LocalDensity.current) {
                    Size(maxWidth.toPx(), maxHeight.toPx())
                }

                var offset: IntOffset by remember { mutableStateOf(IntOffset.Zero) }
                var scale by remember { mutableFloatStateOf(0f) }
                var cropRect by remember { mutableStateOf<Rect?>(null) }

                var isDraggingLeft by remember { mutableStateOf(false) }
                var isDraggingRight by remember { mutableStateOf(false) }
                var isDraggingTop by remember { mutableStateOf(false) }
                var isDraggingBottom by remember { mutableStateOf(false) }
                LaunchedEffect(imageRectProviderFlow) {
                    imageRectProviderFlow.collect {
                        val currentCropRect = cropRect
                        val imageIntrinsicSize = asyncImageState.painter.intrinsicSize

                        if (currentCropRect == null) {
                            it(null)
                            return@collect
                        }

                        // 画像の実際の表示領域を計算
                        val actualImageSize = imageIntrinsicSize * scale
                        val actualImageRect = Rect(
                            left = offset.x.toFloat(),
                            top = offset.y.toFloat(),
                            right = offset.x + actualImageSize.width,
                            bottom = offset.y + actualImageSize.height,
                        )

                        // UI表示座標から実画像座標への変換
                        val imageRelativeCropRect = Rect(
                            left = (currentCropRect.left - actualImageRect.left) / actualImageRect.width,
                            top = (currentCropRect.top - actualImageRect.top) / actualImageRect.height,
                            right = (currentCropRect.right - actualImageRect.left) / actualImageRect.width,
                            bottom = (currentCropRect.bottom - actualImageRect.top) / actualImageRect.height,
                        )

                        // 実画像のピクセル座標に変換
                        val pixelCropRect = Rect(
                            left = imageRelativeCropRect.left * imageIntrinsicSize.width,
                            top = imageRelativeCropRect.top * imageIntrinsicSize.height,
                            right = imageRelativeCropRect.right * imageIntrinsicSize.width,
                            bottom = imageRelativeCropRect.bottom * imageIntrinsicSize.height,
                        )

                        it(pixelCropRect)
                    }
                }

                LaunchedEffect(Unit) {
                    val widthScale = containerSize.width / asyncImageState.painter.intrinsicSize.width
                    val heightScale = containerSize.height / asyncImageState.painter.intrinsicSize.height
                    scale = if (widthScale > heightScale) {
                        heightScale
                    } else {
                        widthScale
                    }

                    val imageSize = asyncImageState.painter.intrinsicSize * scale
                    offset = IntOffset(
                        x = ((containerSize.width - imageSize.width) / 2).toInt(),
                        y = ((containerSize.height - imageSize.height) / 2).toInt(),
                    )

                    cropRect = run {
                        if (initialRect != null) {
                            // 実画像のピクセル座標からUI表示座標への変換
                            val imageIntrinsicSize = asyncImageState.painter.intrinsicSize
                            val imageRelativeCropRect = Rect(
                                left = initialRect.left / imageIntrinsicSize.width,
                                top = initialRect.top / imageIntrinsicSize.height,
                                right = initialRect.right / imageIntrinsicSize.width,
                                bottom = initialRect.bottom / imageIntrinsicSize.height,
                            )

                            // UI表示座標に変換
                            Rect(
                                left = imageRelativeCropRect.left * imageSize.width + offset.x,
                                top = imageRelativeCropRect.top * imageSize.height + offset.y,
                                right = imageRelativeCropRect.right * imageSize.width + offset.x,
                                bottom = imageRelativeCropRect.bottom * imageSize.height + offset.y,
                            )
                        } else {
                            val paddingRatio = (1 - INITIAL_CROP_SIZE_RATIO) / 2
                            Rect(
                                left = imageSize.width * paddingRatio + offset.x,
                                top = imageSize.height * paddingRatio + offset.y,
                                right = imageSize.width * (1 - paddingRatio) + offset.x,
                                bottom = imageSize.height * (1 - paddingRatio) + offset.y,
                            )
                        }
                    }
                }
                androidx.compose.foundation.Canvas(
                    modifier = Modifier.fillMaxSize()
                        .transformable(
                            state = rememberTransformableState { zoomChange, panChange, _ ->
                                scale *= zoomChange
                                offset += IntOffset(
                                    x = panChange.x.toInt(),
                                    y = panChange.y.toInt(),
                                )
                            },
                        )
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val cropRect = cropRect ?: return@detectDragGestures
                                    when {
                                        // top
                                        Rect(
                                            left = cropRect.left - EDGE_DETECTION_THRESHOLD,
                                            right = cropRect.right + EDGE_DETECTION_THRESHOLD,
                                            top = cropRect.top - EDGE_DETECTION_THRESHOLD,
                                            bottom = cropRect.top + EDGE_DETECTION_THRESHOLD,
                                        ).contains(offset) -> {
                                            isDraggingTop = true
                                        }

                                        // bottom
                                        Rect(
                                            left = cropRect.left - EDGE_DETECTION_THRESHOLD,
                                            right = cropRect.right + EDGE_DETECTION_THRESHOLD,
                                            top = cropRect.bottom - EDGE_DETECTION_THRESHOLD,
                                            bottom = cropRect.bottom + EDGE_DETECTION_THRESHOLD,
                                        ).contains(offset) -> {
                                            isDraggingBottom = true
                                        }

                                        // left
                                        Rect(
                                            left = cropRect.left - EDGE_DETECTION_THRESHOLD,
                                            right = cropRect.left + EDGE_DETECTION_THRESHOLD,
                                            top = cropRect.top - EDGE_DETECTION_THRESHOLD,
                                            bottom = cropRect.bottom + EDGE_DETECTION_THRESHOLD,
                                        ).contains(offset) -> {
                                            isDraggingLeft = true
                                        }

                                        // right
                                        Rect(
                                            left = cropRect.right - EDGE_DETECTION_THRESHOLD,
                                            right = cropRect.right + EDGE_DETECTION_THRESHOLD,
                                            top = cropRect.top - EDGE_DETECTION_THRESHOLD,
                                            bottom = cropRect.bottom + EDGE_DETECTION_THRESHOLD,
                                        ).contains(offset) -> {
                                            isDraggingRight = true
                                        }
                                    }
                                },
                                onDragEnd = {
                                    isDraggingLeft = false
                                    isDraggingRight = false
                                    isDraggingTop = false
                                    isDraggingBottom = false
                                },
                                onDrag = { change, dragAmount ->
                                    val rect = cropRect ?: return@detectDragGestures

                                    cropRect = Rect(
                                        left = when {
                                            isDraggingLeft -> (rect.left + dragAmount.x).coerceAtMost(rect.right)
                                            else -> rect.left
                                        },
                                        top = when {
                                            isDraggingTop -> (rect.top + dragAmount.y).coerceAtMost(rect.bottom)
                                            else -> rect.top
                                        },
                                        right = when {
                                            isDraggingRight -> (rect.right + dragAmount.x).coerceAtLeast(rect.left)
                                            else -> rect.right
                                        },
                                        bottom = when {
                                            isDraggingBottom -> (rect.bottom + dragAmount.y).coerceAtLeast(rect.top)
                                            else -> rect.bottom
                                        },
                                    )
                                },
                            )
                        },
                ) {
                    translate(
                        left = offset.x.toFloat(),
                        top = offset.y.toFloat(),
                    ) {
                        with(asyncImageState.painter) {
                            draw(
                                size = (asyncImage).intrinsicSize * scale,
                            )
                        }
                    }
                    when (val cropRect = cropRect) {
                        null -> Unit

                        else -> {
                            drawRect(
                                rect = cropRect,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawRect(rect: Rect) {
    val overlayColor = Color.Black.copy(alpha = 0.5f)
    drawRect(
        color = overlayColor,
        size = Size(rect.left, size.height),
    )
    drawRect(
        color = overlayColor,
        topLeft = Offset(rect.right, 0f),
        size = Size(size.width - rect.right, size.height),
    )
    drawRect(
        color = overlayColor,
        topLeft = Offset(rect.left, 0f),
        size = Size(rect.width, rect.top),
    )
    drawRect(
        color = overlayColor,
        topLeft = Offset(rect.left, rect.bottom),
        size = Size(rect.width, size.height - rect.bottom),
    )

    drawRect(
        color = Color.White,
        topLeft = Offset(rect.left, rect.top),
        size = Size(rect.width, rect.height),
        style = Stroke(width = 2f),
    )

}
