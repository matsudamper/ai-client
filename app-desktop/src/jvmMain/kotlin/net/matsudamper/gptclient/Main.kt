package net.matsudamper.gptclient

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.io.File
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
    val appDirectory = File(System.getProperty("user.home"), ".gpt-client").apply { mkdirs() }

    startKoin {
        loadKoinModules(
            module = module {
                single<AppDatabase> {
                    RoomPlatformBuilder.create(
                        databaseFile = appDirectory.resolve("app-database.db"),
                    )
                }
                single<PlatformRequest> {
                    JvmPlatformRequest(
                        appDirectoryProvider = { appDirectory },
                    )
                }
                single<SettingsEncryptor> {
                    NoopSettingsEncryptor()
                }
                single<SettingDataStore> {
                    SettingDataStore(
                        filename = appDirectory.resolve("setting").absolutePath,
                        encryptor = get(),
                    )
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
            App()
        }
    }
}
