package net.matsudamper.gptclient

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext

class AndroidPlatformRequest(private val activity: ComponentActivity) : PlatformRequest {
    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "gpt_client_notifications"
        private const val NOTIFICATION_ID = 1001
    }

    private var notificationLaunchHandler: ((String) -> Unit)? = null

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

    override suspend fun getMedia(): List<String> {
        val cacheDir = activity.cacheDir
        return mediaLauncher.launch().map { uriString ->
            withContext(Dispatchers.IO) {
                val uri = uriString.toUri()
                val source = ImageDecoder.createSource(activity.contentResolver, uri)
                val bitmap = ImageDecoder.decodeBitmap(source)

                val hash = bitmap.hashCode().toString()
                val file = File(cacheDir, "$hash.png")

                if (!file.exists()) {
                    file.outputStream().use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                }

                file.toURI().toString()
            }
        }
    }

    override suspend fun readPngByteArray(uri: String): ByteArray? {
        return withContext(Dispatchers.IO) {
            val source = ImageDecoder.createSource(activity.contentResolver, uri.toUri())
            val bitmap = try {
                ImageDecoder.decodeBitmap(source)
            } catch (_: FileNotFoundException) {
                return@withContext null
            }

            ByteArrayOutputStream().use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.toByteArray()
            }
        }
    }

    override fun openLink(url: String) {
        activity.startActivity(
            android.content.Intent(
                android.content.Intent.ACTION_VIEW,
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
        viewWidth: Int,
        viewHeight: Int,
    ): String? {
        return withContext(Dispatchers.IO) {
            try {
                val source = ImageDecoder.createSource(activity.contentResolver, uri.toUri())
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
                val file = File(activity.cacheDir, "cropped_$hash.png")

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

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "GPT Client"
            val descriptionText = "GPT Client notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                activity.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun showNotification(title: String, message: String, chatRoomId: String?) {
        val intent = Intent(activity, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (chatRoomId != null) {
                putExtra(MainActivity.KEY_CHATROOM_ID, chatRoomId)
            }
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            activity,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(activity, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(activity)) {
            if (android.content.pm.PackageManager.PERMISSION_GRANTED ==
                androidx.core.content.ContextCompat.checkSelfPermission(
                    activity,
                    android.Manifest.permission.POST_NOTIFICATIONS,
                )
            ) {
                notify(NOTIFICATION_ID, builder.build())
            }
        }
    }

    override fun setNotificationLaunchHandler(handler: (String) -> Unit) {
        notificationLaunchHandler = handler
    }

    override fun handleNotificationLaunch(chatRoomId: String) {
        notificationLaunchHandler?.invoke(chatRoomId)
    }
}
