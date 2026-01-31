package net.matsudamper.gptclient.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Rect
import androidx.core.net.toUri
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BitmapUtils {
    /**
     * Crops a bitmap from a URI using the specified crop rectangle.
     *
     * @param context The Android context
     * @param uri The URI of the image to crop
     * @param cropRect The rectangle to crop (in the coordinate space of the image view)
     * @param viewWidth The width of the view containing the image
     * @param viewHeight The height of the view containing the image
     * @return The URI of the cropped image, or null if cropping failed
     */
    suspend fun cropImageFromUri(
        context: Context,
        uri: String,
        cropRect: androidx.compose.ui.geometry.Rect,
        viewWidth: Int,
        viewHeight: Int,
    ): String? = withContext(Dispatchers.IO) {
        try {
            val source = ImageDecoder.createSource(context.contentResolver, uri.toUri())
            val bitmap = ImageDecoder.decodeBitmap(source)

            // Calculate the actual image dimensions within the view
            val imageWidth = bitmap.width
            val imageHeight = bitmap.height

            // Calculate scaling factors
            val scaleX = imageWidth.toFloat() / viewWidth
            val scaleY = imageHeight.toFloat() / viewHeight

            // Convert view coordinates to bitmap coordinates
            val bitmapCropRect = android.graphics.RectF(
                cropRect.left * scaleX,
                cropRect.top * scaleY,
                cropRect.right * scaleX,
                cropRect.bottom * scaleY,
            )

            // Ensure the crop rect is within the bitmap bounds
            val validLeft = bitmapCropRect.left.coerceIn(0f, imageWidth.toFloat())
            val validTop = bitmapCropRect.top.coerceIn(0f, imageHeight.toFloat())
            val validRight = bitmapCropRect.right.coerceIn(0f, imageWidth.toFloat())
            val validBottom = bitmapCropRect.bottom.coerceIn(0f, imageHeight.toFloat())

            // Create the cropped bitmap
            val croppedBitmap = Bitmap.createBitmap(
                bitmap,
                validLeft.toInt(),
                validTop.toInt(),
                (validRight - validLeft).toInt(),
                (validBottom - validTop).toInt(),
            )

            // Save the cropped bitmap to a file
            val hash = croppedBitmap.hashCode().toString()
            val file = File(context.cacheDir, "cropped_$hash.png")

            file.outputStream().use { outputStream ->
                croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }

            return@withContext file.toURI().toString()
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
}
