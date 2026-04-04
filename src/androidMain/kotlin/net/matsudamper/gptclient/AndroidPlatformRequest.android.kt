package net.matsudamper.gptclient

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import java.io.ByteArrayOutputStream
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
            resultFlow.trySend(it.map { it.toString() })
        }

        suspend fun launch(): List<String> {
            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            return resultFlow.receive()
        }
    }

    override suspend fun getMediaList(): List<String> {
        val cacheDir = activity.cacheDir
        return mediaLauncher.launch().map { uriString ->
            withContext(Dispatchers.IO) {
                val uri = uriString.toUri()
                val source = ImageDecoder.createSource(activity.contentResolver, uri)
                val bitmap = ImageDecoder.decodeBitmap(source)

                val hash = MessageDigest.getInstance("SHA-256")
                    .digest(uriString.toByteArray())
                    .joinToString("") { "%02x".format(it) }
                val file = File(cacheDir, "$hash.webp")

                if (!file.exists()) {
                    file.writeLossyWebp(bitmap)
                }

                file.toURI().toString()
            }
        }
    }

    override suspend fun readImageData(uri: String): PlatformRequest.ImageData? {
        return withContext(Dispatchers.IO) {
            val source = ImageDecoder.createSource(activity.contentResolver, uri.toUri())
            val bitmap = try {
                ImageDecoder.decodeBitmap(source)
            } catch (_: FileNotFoundException) {
                return@withContext null
            }

            PlatformRequest.ImageData(
                bytes = bitmap.toLossyWebpByteArray(),
                mimeType = COMPRESSED_IMAGE_MIME_TYPE,
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
            activity.contentResolver.delete(uri.toUri(), null, null) > 0
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

    override suspend fun cropImage(
        uri: String,
        cropRect: PlatformRequest.CropRect,
    ): String? {
        return withContext(Dispatchers.IO) {
            try {
                val source = ImageDecoder.createSource(activity.contentResolver, uri.toUri())
                val bitmap = ImageDecoder.decodeBitmap(source)

                val imageWidth = bitmap.width
                val imageHeight = bitmap.height

                // cropRectは相対座標(0.0~1.0)で渡されるので実際のピクセル座標に変換
                val bitmapCropRect = android.graphics.RectF(
                    cropRect.left * imageWidth,
                    cropRect.top * imageHeight,
                    cropRect.right * imageWidth,
                    cropRect.bottom * imageHeight,
                )

                val validLeft = bitmapCropRect.left.coerceIn(0f, imageWidth.toFloat())
                val validTop = bitmapCropRect.top.coerceIn(0f, imageHeight.toFloat())
                val validRight = bitmapCropRect.right.coerceIn(0f, imageWidth.toFloat())
                val validBottom = bitmapCropRect.bottom.coerceIn(0f, imageHeight.toFloat())

                // 切り抜き後のビットマップを生成
                val croppedBitmap = Bitmap.createBitmap(
                    bitmap,
                    validLeft.toInt(),
                    validTop.toInt(),
                    (validRight - validLeft).toInt(),
                    (validBottom - validTop).toInt(),
                )

                // 切り抜き後のビットマップをファイルに保存
                val hash = croppedBitmap.hashCode().toString()
                val file = File(activity.cacheDir, "cropped_$hash.webp")

                file.writeLossyWebp(croppedBitmap)

                return@withContext file.toURI().toString()
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
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

    private fun Bitmap.toLossyWebpByteArray(): ByteArray {
        return ByteArrayOutputStream().use { outputStream ->
            compress(Bitmap.CompressFormat.WEBP_LOSSY, WEBP_QUALITY, outputStream)
            outputStream.toByteArray()
        }
    }

    private fun File.writeLossyWebp(bitmap: Bitmap) {
        outputStream().use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, WEBP_QUALITY, outputStream)
        }
    }

    private companion object {
        private const val COMPRESSED_IMAGE_MIME_TYPE = "image/webp"
        private const val WEBP_QUALITY = 75
    }
}
