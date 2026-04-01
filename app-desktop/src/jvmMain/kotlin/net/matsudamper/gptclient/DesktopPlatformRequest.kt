package net.matsudamper.gptclient

import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URI
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DesktopPlatformRequest : PlatformRequest {
    override suspend fun getMediaList(): List<String> = withContext(Dispatchers.IO) {
        var result: List<String> = emptyList()
        SwingUtilities.invokeAndWait {
            val chooser = JFileChooser().apply {
                isMultiSelectionEnabled = true
                fileFilter = FileNameExtensionFilter(
                    "Images",
                    "jpg", "jpeg", "png", "gif", "bmp", "webp",
                )
            }
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                result = chooser.selectedFiles.map { it.toURI().toString() }
            }
        }
        result
    }

    override suspend fun readImageData(uri: String): PlatformRequest.ImageData? {
        return withContext(Dispatchers.IO) {
            runCatching {
                val file = File(URI(uri))
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
                File(URI(uri)).delete()
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
                val file = File(URI(uri))
                val image = ImageIO.read(file) ?: return@withContext null

                val imageWidth = image.width
                val imageHeight = image.height

                val left = (cropRect.left * imageWidth).toInt().coerceIn(0, imageWidth)
                val top = (cropRect.top * imageHeight).toInt().coerceIn(0, imageHeight)
                val right = (cropRect.right * imageWidth).toInt().coerceIn(0, imageWidth)
                val bottom = (cropRect.bottom * imageHeight).toInt().coerceIn(0, imageHeight)

                val cropped = image.getSubimage(left, top, right - left, bottom - top)

                val hash = cropped.hashCode().toString()
                val outputFile = File(System.getProperty("java.io.tmpdir"), "cropped_$hash.jpg")
                ImageIO.write(cropped, "jpg", outputFile)

                outputFile.toURI().toString()
            }.getOrNull()
        }
    }

    override fun createNotificationChannel(channelId: String) {
        // No-op on desktop
    }

    private fun BufferedImage.toJpegByteArray(): ByteArray {
        return ByteArrayOutputStream().use { outputStream ->
            ImageIO.write(this, "jpg", outputStream)
            outputStream.toByteArray()
        }
    }

    private companion object {
        private const val COMPRESSED_IMAGE_MIME_TYPE = "image/jpeg"
    }
}
