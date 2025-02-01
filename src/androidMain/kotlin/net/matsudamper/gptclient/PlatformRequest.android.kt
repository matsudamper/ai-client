package net.matsudamper.gptclient

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

class AndroidPlatformRequest(
    private val activity: ComponentActivity,
) : PlatformRequest {
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
            val bitmap = ImageDecoder.decodeBitmap(source)

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
                url.toUri()
            )
        )
    }

    override suspend fun deleteFile(uri: String): Boolean {
        return withContext(Dispatchers.IO) {
            activity.contentResolver.delete(uri.toUri(), null, null) > 0
        }
    }

    override fun showToast(text: String) {
        Toast.makeText(activity, text, Toast.LENGTH_SHORT).show()
    }
}
