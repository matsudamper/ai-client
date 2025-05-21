package net.matsudamper.gptclient.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import kotlin.math.abs

@Composable
fun ImageCropDialog(
    imageUri: String,
    onDismissRequest: () -> Unit,
    onCropComplete: (cropRect: Rect, imageSize: IntSize) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
        ) {
            var imageSize by remember { mutableStateOf(IntSize.Zero) }
            var cropRect by remember { mutableStateOf<Rect?>(null) }
            var imagePosition by remember { mutableStateOf(Offset.Zero) }
            // Edge detection constants
            val edgeDetectionThreshold = 40f // Threshold for detecting edges
            // Track which edges are being dragged
            var isDraggingLeft by remember { mutableStateOf(false) }
            var isDraggingRight by remember { mutableStateOf(false) }
            var isDraggingTop by remember { mutableStateOf(false) }
            var isDraggingBottom by remember { mutableStateOf(false) }
            var isDraggingEntire by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier.weight(1f)
                    .fillMaxWidth()
                    .padding(top = 16.dp) // Add padding to prevent overlap with NavigationBack
            ) {
                AsyncImage(
                    modifier = Modifier.fillMaxSize()
                        .onGloballyPositioned { coordinates ->
                            imageSize = coordinates.size
                            imagePosition = coordinates.positionInWindow()

                            // Initialize crop rect to center 80% of the image
                            if (cropRect == null && imageSize.width > 0 && imageSize.height > 0) {
                                val width = imageSize.width * 0.8f
                                val height = imageSize.height * 0.8f
                                val left = (imageSize.width - width) / 2
                                val top = (imageSize.height - height) / 2
                                cropRect = Rect(left, top, left + width, top + height)
                            }
                        }
                        .drawWithContent {
                            drawContent()

                            val rect = cropRect ?: return@drawWithContent

                            // Draw semi-transparent overlay outside crop area
                            drawRect(
                                color = Color.Black.copy(alpha = 0.5f),
                                size = Size(rect.left, size.height)
                            )
                            drawRect(
                                color = Color.Black.copy(alpha = 0.5f),
                                topLeft = Offset(rect.right, 0f),
                                size = Size(size.width - rect.right, size.height)
                            )
                            drawRect(
                                color = Color.Black.copy(alpha = 0.5f),
                                topLeft = Offset(rect.left, 0f),
                                size = Size(rect.width, rect.top)
                            )
                            drawRect(
                                color = Color.Black.copy(alpha = 0.5f),
                                topLeft = Offset(rect.left, rect.bottom),
                                size = Size(rect.width, size.height - rect.bottom)
                            )

                            // Draw crop rectangle border
                            drawRect(
                                color = Color.White,
                                topLeft = Offset(rect.left, rect.top),
                                size = Size(rect.width, rect.height),
                                style = Stroke(width = 2f)
                            )
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    // Determine which edge is being dragged at the start
                                    cropRect?.let { rect ->
                                        // Check if we're near the edges
                                        isDraggingLeft = abs(offset.x - rect.left) < edgeDetectionThreshold
                                        isDraggingRight = abs(offset.x - rect.right) < edgeDetectionThreshold
                                        isDraggingTop = abs(offset.y - rect.top) < edgeDetectionThreshold
                                        isDraggingBottom = abs(offset.y - rect.bottom) < edgeDetectionThreshold

                                        // If not dragging any edge, check if we're inside the rectangle
                                        if (!isDraggingLeft && !isDraggingRight && !isDraggingTop && !isDraggingBottom) {
                                            isDraggingEntire = offset.x > rect.left && offset.x < rect.right && 
                                                              offset.y > rect.top && offset.y < rect.bottom
                                        }
                                    }
                                },
                                onDragEnd = {
                                    // Reset dragging state
                                    isDraggingLeft = false
                                    isDraggingRight = false
                                    isDraggingTop = false
                                    isDraggingBottom = false
                                    isDraggingEntire = false
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                cropRect?.let { rect ->
                                    // Calculate new rectangle dimensions based on which edge is being dragged
                                    var newLeft = rect.left
                                    var newTop = rect.top
                                    var newRight = rect.right
                                    var newBottom = rect.bottom

                                    if (isDraggingEntire) {
                                        // Move the entire rectangle
                                        val rectWidth = rect.width
                                        val rectHeight = rect.height

                                        // Calculate new positions while keeping the rectangle within bounds
                                        newLeft = (rect.left + dragAmount.x).coerceIn(0f, imageSize.width - rectWidth)
                                        newRight = newLeft + rectWidth

                                        newTop = (rect.top + dragAmount.y).coerceIn(0f, imageSize.height - rectHeight)
                                        newBottom = newTop + rectHeight
                                    } else {
                                        // Handle edge dragging
                                        if (isDraggingLeft) {
                                            newLeft = (rect.left + dragAmount.x).coerceIn(0f, rect.right - edgeDetectionThreshold)
                                        }
                                        if (isDraggingRight) {
                                            newRight = (rect.right + dragAmount.x).coerceIn(rect.left + edgeDetectionThreshold, imageSize.width.toFloat())
                                        }
                                        if (isDraggingTop) {
                                            newTop = (rect.top + dragAmount.y).coerceIn(0f, rect.bottom - edgeDetectionThreshold)
                                        }
                                        if (isDraggingBottom) {
                                            newBottom = (rect.bottom + dragAmount.y).coerceIn(rect.top + edgeDetectionThreshold, imageSize.height.toFloat())
                                        }
                                    }

                                    // Update crop rectangle
                                    cropRect = Rect(
                                        left = newLeft,
                                        top = newTop,
                                        right = newRight,
                                        bottom = newBottom
                                    )
                                }
                            }
                        },
                    model = imageUri,
                    contentScale = ContentScale.Fit,
                    contentDescription = null,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        cropRect?.let { rect ->
                            onCropComplete(rect, imageSize)
                        }
                        onDismissRequest()
                    }
                ) {
                    Text("Complete")
                }
            }
        }
    }
}
