package net.matsudamper.gptclient

import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.map.Mapper
import coil3.request.Options
import java.io.File
import java.net.URI
import kotlin.system.exitProcess
import net.matsudamper.gptclient.datastore.NoopSettingsEncryptor
import net.matsudamper.gptclient.datastore.SettingDataStore
import net.matsudamper.gptclient.datastore.SettingsEncryptor
import net.matsudamper.gptclient.room.AppDatabase
import net.matsudamper.gptclient.room.RoomPlatformBuilder
import net.matsudamper.gptclient.viewmodel.AddRequestUseCase
import net.matsudamper.gptclient.worker.JvmWorkManagerScheduler
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun main(@Suppress("UNUSED_PARAMETER") args: Array<String>) {
    val appDatabasePath = JvmAppStorage.resolve("app-database").absolutePath
    val settingDataStorePath = JvmAppStorage.resolve("setting.pb").absolutePath
    val desktopPlatformRequest = DesktopPlatformRequest()

    SingletonImageLoader.setSafe(::createDesktopImageLoader)

    startKoin {
        loadKoinModules(
            module = module {
                single<AppDatabase> {
                    RoomPlatformBuilder.create(appDatabasePath)
                }
                single<SettingsEncryptor> {
                    NoopSettingsEncryptor()
                }
                single<SettingDataStore> {
                    SettingDataStore(
                        storagePath = settingDataStorePath,
                        encryptor = get(),
                    )
                }
                single<PlatformRequest> {
                    desktopPlatformRequest
                }
                single<AddRequestUseCase.WorkManagerScheduler> {
                    JvmWorkManagerScheduler(
                        appDatabase = get(),
                        platformRequest = get(),
                        settingDataStore = get(),
                    )
                }
            },
        )
    }
    application {
        Window(onCloseRequest = { exitProcess(0) }) {
            App(
                providePlatformRequest = remember {
                    { desktopPlatformRequest }
                },
            )
        }
    }
}

private fun createDesktopImageLoader(context: coil3.PlatformContext): ImageLoader {
    return ImageLoader.Builder(context)
        .components {
            // Desktopでは画像選択後に file:// URI を文字列で保持しているため、
            // Coil が読める File に変換する。
            add(
                Mapper<String, File> { data, _: Options ->
                    when {
                        data.startsWith("file:", ignoreCase = true) -> {
                            runCatching { File(URI(data)) }.getOrNull()?.takeIf { it.isFile }
                        }

                        else -> File(data).takeIf { it.isAbsolute && it.isFile }
                    }
                },
            )
        }
        .build()
}
