package net.matsudamper.gptclient

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import net.matsudamper.gptclient.datastore.SettingDataStore

fun main(args: Array<String>) {
    application {
        Window(onCloseRequest = {}) {
            App(
                settingDataStore = SettingDataStore(
                    filename = "setting"
                )
            )
        }
    }
}
