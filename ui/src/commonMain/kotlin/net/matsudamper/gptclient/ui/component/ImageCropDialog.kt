package net.matsudamper.gptclient.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.abs
import coil3.compose.AsyncImage
import net.matsudamper.gptclient.ui.systemGestureExclusion

private const val EDGE_DETECTION_THRESHOLD = 40f
private const val INITIAL_CROP_SIZE_RATIO = 0.8f

@Composable
fun ImageCropDialog(
    imageUri: String,
    rect: Rect?,
    onDismissRequest: () -> Unit,
    onCropComplete: (cropRect: Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
        ),
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f)),
        ) {
            var containerSize by remember { mutableStateOf(IntSize.Zero) }
            var intrinsicImageSize by remember { mutableStateOf(IntSize.Zero) }
            var cropRect: Rect? by remember(rect) { mutableStateOf(rect) }

            var isDraggingLeft by remember { mutableStateOf(false) }
            var isDraggingRight by remember { mutableStateOf(false) }
            var isDraggingTop by remember { mutableStateOf(false) }
            var isDraggingBottom by remember { mutableStateOf(false) }
            var isDraggingEntire by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .systemGestureExclusion()
                    .padding(top = 16.dp),
            ) {
                AsyncImage(
                    modifier = Modifier.fillMaxSize()
                        .onGloballyPositioned { coordinates ->
                            containerSize = coordinates.size
                        }
                        .drawRect(rect = cropRect)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val rect = cropRect
                                    if (rect != null) {
                                        isDraggingLeft = abs(offset.x - rect.left) < EDGE_DETECTION_THRESHOLD
                                        isDraggingRight = abs(offset.x - rect.right) < EDGE_DETECTION_THRESHOLD
                                        isDraggingTop = abs(offset.y - rect.top) < EDGE_DETECTION_THRESHOLD
                                        isDraggingBottom = abs(offset.y - rect.bottom) < EDGE_DETECTION_THRESHOLD

                                        if (!isDraggingLeft && !isDraggingRight && !isDraggingTop && !isDraggingBottom) {
                                            isDraggingEntire = offset.x > rect.left &&
                                                    offset.x < rect.right &&
                                                    offset.y > rect.top &&
                                                    offset.y < rect.bottom
                                        }
                                    }
                                },
                                onDragEnd = {
                                    isDraggingLeft = false
                                    isDraggingRight = false
                                    isDraggingTop = false
                                    isDraggingBottom = false
                                    isDraggingEntire = false
                                },
                            ) { change, dragAmount ->
                                change.consume()
                                val actualImageRect = getActualImageRect(containerSize, intrinsicImageSize)
                                if (actualImageRect == null) return@detectDragGestures

                                val rect = cropRect
                                if (rect != null) {
                                    var newLeft = rect.left
                                    var newTop = rect.top
                                    var newRight = rect.right
                                    var newBottom = rect.bottom

                                    if (isDraggingEntire) {
                                        val rectWidth = rect.width
                                        val rectHeight = rect.height

                                        newLeft = (rect.left + dragAmount.x).coerceIn(
                                            actualImageRect.left,
                                            actualImageRect.right - rectWidth,
                                        )
                                        newRight = newLeft + rectWidth

                                        newTop = (rect.top + dragAmount.y).coerceIn(
                                            actualImageRect.top,
                                            actualImageRect.bottom - rectHeight,
                                        )
                                        newBottom = newTop + rectHeight
                                    } else {
                                        if (isDraggingLeft) {
                                            newLeft = (rect.left + dragAmount.x).coerceIn(
                                                actualImageRect.left,
                                                rect.right - EDGE_DETECTION_THRESHOLD,
                                            )
                                        }
                                        if (isDraggingRight) {
                                            newRight = (rect.right + dragAmount.x).coerceIn(
                                                rect.left + EDGE_DETECTION_THRESHOLD,
                                                actualImageRect.right,
                                            )
                                        }
                                        if (isDraggingTop) {
                                            newTop = (rect.top + dragAmount.y).coerceIn(
                                                actualImageRect.top,
                                                rect.bottom - EDGE_DETECTION_THRESHOLD,
                                            )
                                        }
                                        if (isDraggingBottom) {
                                            newBottom = (rect.bottom + dragAmount.y).coerceIn(
                                                rect.top + EDGE_DETECTION_THRESHOLD,
                                                actualImageRect.bottom,
                                            )
                                        }
                                    }

                                    cropRect = Rect(
                                        left = newLeft,
                                        top = newTop,
                                        right = newRight,
                                        bottom = newBottom,
                                    )
                                }
                            }
                        },
                    model = imageUri,
                    contentScale = ContentScale.Fit,
                    contentDescription = null,
                    onSuccess = { state ->
                        intrinsicImageSize = IntSize(
                            state.painter.intrinsicSize.width.toInt(),
                            state.painter.intrinsicSize.height.toInt(),
                        )

                        // 画像読み込み完了後にクロップ矩形を初期化
                        val actualImageRect = getActualImageRect(containerSize, intrinsicImageSize)
                        if (cropRect == null && actualImageRect != null) {
                            val width = actualImageRect.width * INITIAL_CROP_SIZE_RATIO
                            val height = actualImageRect.height * INITIAL_CROP_SIZE_RATIO
                            val left = actualImageRect.left + (actualImageRect.width - width) / 2
                            val top = actualImageRect.top + (actualImageRect.height - height) / 2
                            cropRect = Rect(left, top, left + width, top + height)
                        }
                    },
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

                Button(
                    onClick = {
                        val rect = cropRect
                        if (rect != null) {
                            val actualImageRect = getActualImageRect(containerSize, intrinsicImageSize)
                            if (actualImageRect != null) {
                                // 表示座標から実画像座標への変換
                                val imageRelativeCropRect = Rect(
                                    left = (rect.left - actualImageRect.left) / actualImageRect.width,
                                    top = (rect.top - actualImageRect.top) / actualImageRect.height,
                                    right = (rect.right - actualImageRect.left) / actualImageRect.width,
                                    bottom = (rect.bottom - actualImageRect.top) / actualImageRect.height,
                                )

                                // 実画像のピクセル座標に変換
                                val pixelCropRect = Rect(
                                    left = imageRelativeCropRect.left * intrinsicImageSize.width,
                                    top = imageRelativeCropRect.top * intrinsicImageSize.height,
                                    right = imageRelativeCropRect.right * intrinsicImageSize.width,
                                    bottom = imageRelativeCropRect.bottom * intrinsicImageSize.height,
                                )

                                onCropComplete(pixelCropRect)
                            }
                        }
                        onDismissRequest()
                    },
                ) {
                    Text("Complete")
                }
            }
        }
    }
}

private fun Modifier.drawRect(rect: Rect?): Modifier {
    return drawWithContent {
        drawContent()

        rect ?: return@drawWithContent
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
}

private fun getActualImageRect(
    containerSize: IntSize,
    intrinsicImageSize: IntSize,
): Rect? {
    if (containerSize.width == 0 ||
        containerSize.height == 0 ||
        intrinsicImageSize.width == 0 ||
        intrinsicImageSize.height == 0
    ) {
        return null
    }

    val containerAspectRatio = containerSize.width.toFloat() / containerSize.height.toFloat()
    val imageAspectRatio = intrinsicImageSize.width.toFloat() / intrinsicImageSize.height.toFloat()

    return if (imageAspectRatio > containerAspectRatio) {
        // 横長画像は幅に合わせる
        val displayHeight = containerSize.width / imageAspectRatio
        val offsetY = (containerSize.height - displayHeight) / 2
        Rect(
            left = 0f,
            top = offsetY,
            right = containerSize.width.toFloat(),
            bottom = offsetY + displayHeight,
        )
    } else {
        // 縦長画像は高さに合わせる
        val displayWidth = containerSize.height * imageAspectRatio
        val offsetX = (containerSize.width - displayWidth) / 2
        Rect(
            left = offsetX,
            top = 0f,
            right = offsetX + displayWidth,
            bottom = containerSize.height.toFloat(),
        )
    }
}
