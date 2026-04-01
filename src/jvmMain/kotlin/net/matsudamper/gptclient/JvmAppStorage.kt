package net.matsudamper.gptclient

import java.io.File

object JvmAppStorage {
    private const val appDirectoryName = "gpt-client"

    fun resolve(filename: String): File {
        return appDirectory().resolve(filename)
    }

    private fun appDirectory(): File {
        val userHome = System.getProperty("user.home")
        val baseDirectory = when {
            isWindows() -> {
                System.getenv("LOCALAPPDATA")?.let(::File)
                    ?: System.getenv("APPDATA")?.let(::File)
                    ?: File(userHome, "AppData/Local")
            }

            isMac() -> File(userHome, "Library/Application Support")
            else -> {
                System.getenv("XDG_DATA_HOME")
                    ?.takeIf(String::isNotBlank)
                    ?.let(::File)
                    ?: File(userHome, ".local/share")
            }
        }
        return baseDirectory.resolve(appDirectoryName).also { directory ->
            check(directory.exists() || directory.mkdirs()) {
                "Failed to create app storage directory: ${directory.absolutePath}"
            }
        }
    }

    private fun isWindows(): Boolean =
        System.getProperty("os.name").startsWith("Windows", ignoreCase = true)

    private fun isMac(): Boolean =
        System.getProperty("os.name").startsWith("Mac", ignoreCase = true)
}
