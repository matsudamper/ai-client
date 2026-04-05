package net.matsudamper.gptclient

import java.awt.EventQueue
import java.awt.FileDialog
import java.awt.Frame
import java.io.FilenameFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DesktopMediaRequest : MediaRequest {
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
                result = dialog.files.map { it.absolutePath }
            } finally {
                owner.dispose()
            }
        }
        result
    }

    private companion object {
        private val supportedImageExtensions = listOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
    }
} 