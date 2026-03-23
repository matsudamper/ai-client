package net.matsudamper.gptclient

import java.awt.Desktop
import java.awt.GraphicsEnvironment
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import java.awt.datatransfer.StringSelection
import java.awt.image.BufferedImage
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.security.MessageDigest
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JvmPlatformRequest(
    private val appDirectoryProvider: () -> File,
) : PlatformRequest {
    private var trayIcon: TrayIcon? = null

    override suspend fun getMediaList(): List<String> = withContext(Dispatchers.IO) {
        val chooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.FILES_ONLY
            isMultiSelectionEnabled = true
        }
        val result = chooser.showOpenDialog(null)
        if (result != JFileChooser.APPROVE_OPTION) {
            return@withContext emptyList()
        }

        val cacheDir = appDirectoryProvider().resolve("cache").apply { mkdirs() }
        chooser.selectedFiles.mapNotNull { sourceFile ->
            if (!sourceFile.exists()) return@mapNotNull null

            val hash = MessageDigest.getInstance("SHA-256")
                .digest(sourceFile.absolutePath.toByteArray())
                .joinToString("") { "%02x".format(it) }
            val cacheFile = cacheDir.resolve("$hash.png")
            if (!cacheFile.exists()) {
                val image = ImageIO.read(sourceFile) ?: return@mapNotNull null
                ImageIO.write(image, "png", cacheFile)
            }
            cacheFile.toURI().toString()
        }
    }

    override suspend fun readPngByteArray(uri: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            URI(uri).let(::File).inputStream().use { input ->
                input.readBytes()
            }
        }.getOrNull()
    }

    override fun openLink(url: String) {
        runCatching {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI(url))
            }
        }
    }

    override suspend fun deleteFile(uri: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            Files.deleteIfExists(URI(uri).let(::File).toPath())
        }.getOrDefault(false)
    }

    override fun showToast(text: String) {
        val icon = getOrCreateTrayIcon()
        if (icon != null) {
            icon.displayMessage("GPT Client", text, TrayIcon.MessageType.INFO)
        } else {
            println(text)
        }
    }

    override fun copyToClipboard(text: String) {
        runCatching {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(StringSelection(text), null)
        }
    }

    override suspend fun cropImage(
        uri: String,
        cropRect: PlatformRequest.CropRect,
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val file = URI(uri).let(::File)
            val image = ImageIO.read(file) ?: return@runCatching null

            val validLeft = cropRect.left.toInt().coerceIn(0, image.width)
            val validTop = cropRect.top.toInt().coerceIn(0, image.height)
            val validRight = cropRect.right.toInt().coerceIn(validLeft + 1, image.width)
            val validBottom = cropRect.bottom.toInt().coerceIn(validTop + 1, image.height)

            val cropped = image.getSubimage(
                validLeft,
                validTop,
                validRight - validLeft,
                validBottom - validTop,
            )

            val output = appDirectoryProvider().resolve("cache").apply { mkdirs() }
                .resolve("cropped_${cropped.hashCode()}.png")
            ImageIO.write(cropped, "png", output)
            output.toURI().toString()
        }.getOrNull()
    }

    override fun createNotificationChannel(channelId: String) {
        getOrCreateTrayIcon()
    }

    private fun getOrCreateTrayIcon(): TrayIcon? {
        val currentIcon = trayIcon
        if (currentIcon != null) return currentIcon

        if (GraphicsEnvironment.isHeadless() || !SystemTray.isSupported()) {
            return null
        }

        val tray = SystemTray.getSystemTray()
        val image = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
        val icon = TrayIcon(image, "GPT Client").apply {
            isImageAutoSize = true
        }

        return runCatching {
            tray.add(icon)
            icon
        }.getOrNull()?.also { trayIcon = it }
    }
}
