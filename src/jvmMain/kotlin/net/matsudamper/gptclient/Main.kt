package net.matsudamper.gptclient

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import net.matsudamper.gptclient.datastore.SettingDataStore
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    application {
        Window(onCloseRequest = { exitProcess(0) }) {
            App(
                settingDataStore = SettingDataStore(
                    filename = "setting"
                )
            )
        }
    }
}
