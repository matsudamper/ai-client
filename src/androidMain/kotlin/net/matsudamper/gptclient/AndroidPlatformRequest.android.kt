package net.matsudamper.gptclient

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toFile
import androidx.core.net.toUri
import java.io.File
import java.io.FileNotFoundException
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext

class AndroidPlatformRequest(private val activity: ComponentActivity) : PlatformRequest {
    private val mediaLauncher = object {
        private val resultFlow = Channel<List<String>>(Channel.RENDEZVOUS)
        private val launcher = activity.registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) {
            resultFlow.trySend(it.map { uri -> uri.toString() })
        }

        suspend fun launch(): List<String> {
            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            return resultFlow.receive()
        }
    }

    override suspend fun getMediaList(): List<String> = mediaLauncher.launch()

    override suspend fun readImageData(uri: String): PlatformRequest.ImageData? {
        return withContext(Dispatchers.IO) {
            val parsedUri = uri.toUri()
            val bytes = try {
                activity.contentResolver.openInputStream(parsedUri)?.use { input -> input.readBytes() }
            } catch (_: FileNotFoundException) {
                null
            } ?: return@withContext null

            val mimeType = activity.contentResolver.getType(parsedUri)
                ?: parsedUri.toMimeType()
                ?: return@withContext null

            PlatformRequest.ImageData(
                bytes = bytes,
                mimeType = mimeType,
            )
        }
    }

    override fun openLink(url: String) {
        activity.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                url.toUri(),
            ),
        )
    }

    override suspend fun deleteFile(uri: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val parsedUri = uri.toUri()
            when (parsedUri.scheme) {
                "file" -> parsedUri.toFile().delete()
                else -> activity.contentResolver.delete(parsedUri, null, null) > 0
            }
        }.getOrNull() == true
    }

    override fun showToast(text: String) {
        Toast.makeText(activity, text, Toast.LENGTH_SHORT).show()
    }

    override fun copyToClipboard(text: String) {
        val clipboard = activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Copied Text", text)
        clipboard.setPrimaryClip(clip)
    }

    override suspend fun prepareImage(
        uri: String,
        cropRect: PlatformRequest.CropRect?,
        imageFormat: ImageFormat,
    ): String? {
        return withContext(Dispatchers.IO) {
            try {
                val source = ImageDecoder.createSource(activity.contentResolver, uri.toUri())
                val bitmap = ImageDecoder.decodeBitmap(source)
                val outputBitmap = cropRect?.let { bitmap.crop(it) } ?: bitmap
                val cacheKey = buildString {
                    append(uri)
                    append('|')
                    append(cropRect?.left)
                    append(',')
                    append(cropRect?.top)
                    append(',')
                    append(cropRect?.right)
                    append(',')
                    append(cropRect?.bottom)
                    append('|')
                    append(imageFormat.name)
                }.sha256Hex()
                val file = File(activity.cacheDir, "$cacheKey.${imageFormat.fileExtension}")

                if (!file.exists()) {
                    file.writeBitmap(outputBitmap, imageFormat)
                }

                file.toURI().toString()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    override fun createNotificationChannel(channelId: String) {
        val name = "GPT Client"
        val descriptionText = "GPT Client notifications"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            activity.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun Bitmap.crop(cropRect: PlatformRequest.CropRect): Bitmap {
        val bitmapCropRect = android.graphics.RectF(
            cropRect.left * width,
            cropRect.top * height,
            cropRect.right * width,
            cropRect.bottom * height,
        )
        val validLeft = bitmapCropRect.left.coerceIn(0f, width.toFloat())
        val validTop = bitmapCropRect.top.coerceIn(0f, height.toFloat())
        val validRight = bitmapCropRect.right.coerceIn(0f, width.toFloat())
        val validBottom = bitmapCropRect.bottom.coerceIn(0f, height.toFloat())
        val cropWidth = (validRight - validLeft).toInt()
        val cropHeight = (validBottom - validTop).toInt()

        if (cropWidth <= 0 || cropHeight <= 0) {
            return this
        }

        return Bitmap.createBitmap(
            this,
            validLeft.toInt(),
            validTop.toInt(),
            cropWidth,
            cropHeight,
        )
    }

    private fun File.writeBitmap(
        bitmap: Bitmap,
        imageFormat: ImageFormat,
    ) {
        outputStream().use { outputStream ->
            val compressFormat = when (imageFormat) {
                ImageFormat.Jpeg -> Bitmap.CompressFormat.JPEG
                ImageFormat.Png -> Bitmap.CompressFormat.PNG
                ImageFormat.Webp -> Bitmap.CompressFormat.WEBP_LOSSY
            }
            bitmap.compress(compressFormat, LOSSY_IMAGE_QUALITY, outputStream)
        }
    }

    private fun Uri.toMimeType(): String? {
        val extension = runCatching {
            when (scheme) {
                "file" -> toFile().extension
                else -> path?.substringAfterLast('.', "")
            }
        }.getOrNull()
            .orEmpty()
            .lowercase()

        return when (extension) {
            "jpg", "jpeg" -> ImageFormat.Jpeg.mimeType
            "png" -> ImageFormat.Png.mimeType
            "webp" -> ImageFormat.Webp.mimeType
            else -> null
        }
    }

    private fun String.sha256Hex(): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private companion object {
        private const val LOSSY_IMAGE_QUALITY = 85
    }
}
