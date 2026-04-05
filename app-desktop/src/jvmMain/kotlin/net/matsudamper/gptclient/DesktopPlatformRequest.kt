package net.matsudamper.gptclient

import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.image.BufferedImage
import java.io.File
import java.net.URI
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.imageio.ImageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DesktopPlatformRequest : PlatformRequest {
    override suspend fun readImageData(uri: String): PlatformRequest.ImageData? {
        return withContext(Dispatchers.IO) {
            runCatching {
                val file = File(uri)
                val mimeType = file.toMimeType() ?: return@withContext null
                PlatformRequest.ImageData(
                    bytes = file.readBytes(),
                    mimeType = mimeType,
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

    override suspend fun prepareImage(
        uri: String,
        cropRect: PlatformRequest.CropRect?,
        imageFormat: ImageFormat,
    ): String? {
        return withContext(Dispatchers.IO) {
            runCatching {
                val file = File(uri)
                val image = ImageIO.read(file) ?: return@withContext null
                val outputImage = cropRect?.let { image.crop(it) } ?: image
                val outputFormat = imageFormat.toWritableFormat()
                val hash = buildString {
                    append(file.absolutePath)
                    append('|')
                    append(cropRect?.left)
                    append(',')
                    append(cropRect?.top)
                    append(',')
                    append(cropRect?.right)
                    append(',')
                    append(cropRect?.bottom)
                    append('|')
                    append(outputFormat.name)
                    append('|')
                    append(file.lastModified())
                    append('|')
                    append(file.sha256Hex())
                }.sha256Hex()
                val outputFile = File(
                    System.getProperty("java.io.tmpdir"),
                    "prepared_$hash.${outputFormat.fileExtension}",
                )

                if (!outputFile.exists()) {
                    outputFile.writeImage(outputImage, outputFormat)
                }

                outputFile.absolutePath
            }.getOrNull()
        }
    }

    override fun createNotificationChannel(channelId: String) {
        // デスクトップでは何もしない
    }

    private fun BufferedImage.crop(cropRect: PlatformRequest.CropRect): BufferedImage {
        val left = (cropRect.left * width).toInt().coerceIn(0, width)
        val top = (cropRect.top * height).toInt().coerceIn(0, height)
        val right = (cropRect.right * width).toInt().coerceIn(0, width)
        val bottom = (cropRect.bottom * height).toInt().coerceIn(0, height)
        val cropWidth = right - left
        val cropHeight = bottom - top

        if (cropWidth <= 0 || cropHeight <= 0) {
            return this
        }

        return getSubimage(left, top, cropWidth, cropHeight)
    }

    private fun File.writeImage(
        image: BufferedImage,
        imageFormat: ImageFormat,
    ) {
        val writerFormat = when (imageFormat) {
            ImageFormat.Jpeg -> "jpg"
            ImageFormat.Png -> "png"
            ImageFormat.Webp -> error("DesktopPlatformRequest does not support WebP output")
        }
        ImageIO.write(image, writerFormat, this)
    }

    private fun ImageFormat.toWritableFormat(): ImageFormat {
        return when (this) {
            ImageFormat.Webp -> ImageFormat.Png
            ImageFormat.Jpeg,
            ImageFormat.Png,
            -> this
        }
    }

    private fun File.toMimeType(): String? {
        return when (extension.lowercase()) {
            "jpg", "jpeg" -> ImageFormat.Jpeg.mimeType
            "png" -> ImageFormat.Png.mimeType
            "webp" -> ImageFormat.Webp.mimeType
            else -> null
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
}
