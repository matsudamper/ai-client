package net.matsudamper.gptclient

import java.awt.Desktop
import java.awt.EventQueue
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FilenameFilter
import java.net.URI
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.imageio.ImageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DesktopPlatformRequest : PlatformRequest {
    override suspend fun getMediaList(): List<String> = withContext(Dispatchers.IO) {
        var result: List<String> = emptyList()
        EventQueue.invokeAndWait {
            val owner = Frame()
            try {
                val dialog = FileDialog(owner, "画像を選択", FileDialog.LOAD).apply {
                    isMultipleMode = true
                    filenameFilter = FilenameFilter { _, name ->
                        supportedImageExtensions.any { extension ->
                            name.endsWith(".$extension", ignoreCase = true)
                        }
                    }
                }
                dialog.isVisible = true
                result = dialog.files
                    .map { it.absolutePath }
            } finally {
                owner.dispose()
            }
        }
        result
    }

    override suspend fun readImageData(uri: String): PlatformRequest.ImageData? {
        return withContext(Dispatchers.IO) {
            runCatching {
                val file = File(uri)
                val image = ImageIO.read(file) ?: return@withContext null
                PlatformRequest.ImageData(
                    bytes = image.toJpegByteArray(),
                    mimeType = COMPRESSED_IMAGE_MIME_TYPE,
                )
            }.getOrNull()
        }
    }

    override fun openLink(url: String) {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(url))
        }
    }

    override suspend fun deleteFile(uri: String): Boolean {
        return withContext(Dispatchers.IO) {
            runCatching {
                File(uri).delete()
            }.getOrNull() == true
        }
    }

    override fun showToast(text: String) {
        System.err.println("[Toast] $text")
    }

    override fun copyToClipboard(text: String) {
        val selection = StringSelection(text)
        Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
    }

    override suspend fun cropImage(
        uri: String,
        cropRect: PlatformRequest.CropRect,
    ): String? {
        return withContext(Dispatchers.IO) {
            runCatching {
                val file = File(uri)
                val image = ImageIO.read(file) ?: return@withContext null

                val imageWidth = image.width
                val imageHeight = image.height

                val left = (cropRect.left * imageWidth).toInt().coerceIn(0, imageWidth)
                val top = (cropRect.top * imageHeight).toInt().coerceIn(0, imageHeight)
                val right = (cropRect.right * imageWidth).toInt().coerceIn(0, imageWidth)
                val bottom = (cropRect.bottom * imageHeight).toInt().coerceIn(0, imageHeight)
                val cropWidth = right - left
                val cropHeight = bottom - top

                val cropped = image.getSubimage(left, top, cropWidth, cropHeight)

                val hash = buildString {
                    append(file.toURI())
                    append('|')
                    append(left)
                    append(',')
                    append(top)
                    append(',')
                    append(cropWidth)
                    append(',')
                    append(cropHeight)
                    append('|')
                    append(file.lastModified())
                    append('|')
                    append(file.sha256Hex())
                }.sha256Hex()
                val outputFile = File(System.getProperty("java.io.tmpdir"), "cropped_$hash.jpg")
                if (!outputFile.exists()) {
                    ImageIO.write(cropped, "jpg", outputFile)
                }

                outputFile.absolutePath
            }.getOrNull()
        }
    }

    override fun createNotificationChannel(channelId: String) {
        // デスクトップでは何もしない
    }

    private fun BufferedImage.toJpegByteArray(): ByteArray {
        return ByteArrayOutputStream().use { outputStream ->
            ImageIO.write(this, "jpg", outputStream)
            outputStream.toByteArray()
        }
    }

    private fun File.sha256Hex(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) {
                    break
                }
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().toHexString()
    }

    private fun String.sha256Hex(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(toByteArray(StandardCharsets.UTF_8)).toHexString()
    }

    private fun ByteArray.toHexString(): String {
        return joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    private companion object {
        private const val COMPRESSED_IMAGE_MIME_TYPE = "image/jpeg"
        private val supportedImageExtensions = listOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
    }
}
