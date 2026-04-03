package net.matsudamper.gptclient

import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlin.system.exitProcess
import net.matsudamper.gptclient.datastore.NoopSettingsEncryptor
import net.matsudamper.gptclient.datastore.SettingDataStore
import net.matsudamper.gptclient.datastore.SettingsEncryptor
import net.matsudamper.gptclient.localmodel.LocalModelRepository
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
                single<LocalModelRepository> {
                    LocalModelRepository()
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
