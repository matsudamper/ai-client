package net.matsudamper.gptclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import net.matsudamper.gptclient.datastore.SettingDataStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App(
                settingDataStore = remember {
                    SettingDataStore(filesDir.resolve("setting").absolutePath)
                }
            )
        }
    }
}
