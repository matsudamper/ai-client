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
            
            Box(
                modifier = Modifier.weight(1f)
                    .fillMaxWidth()
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
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                cropRect?.let { rect ->
                                    // Update crop rectangle position
                                    val newLeft = (rect.left + dragAmount.x).coerceIn(0f, imageSize.width - rect.width)
                                    val newTop = (rect.top + dragAmount.y).coerceIn(0f, imageSize.height - rect.height)
                                    cropRect = Rect(
                                        left = newLeft,
                                        top = newTop,
                                        right = newLeft + rect.width,
                                        bottom = newTop + rect.height
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