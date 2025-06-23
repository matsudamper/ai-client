package net.matsudamper.gptclient

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlin.system.exitProcess
import net.matsudamper.gptclient.datastore.SettingDataStore
import net.matsudamper.gptclient.room.AppDatabase
import net.matsudamper.gptclient.room.RoomPlatformBuilder
import net.matsudamper.gptclient.viewmodel.AddRequestUseCase
import net.matsudamper.gptclient.worker.JvmWorkManagerScheduler
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun main(@Suppress("UNUSED_PARAMETER") args: Array<String>) {
    startKoin {
        loadKoinModules(
            module = module {
                single<AppDatabase> {
                    RoomPlatformBuilder.create()
                }
                single<SettingDataStore> {
                    SettingDataStore(
                        filename = "setting",
                    )
                }
                single<AddRequestUseCase.WorkManagerScheduler> {
                    JvmWorkManagerScheduler()
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
