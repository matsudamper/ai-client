package net.matsudamper.gptclient

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.runtime.mutableStateOf
import net.matsudamper.gptclient.localmodel.EXTRA_OPEN_LOCAL_MODEL_SETTINGS
import net.matsudamper.gptclient.navigation.Navigator
import net.matsudamper.gptclient.room.entity.ChatRoomId
import org.koin.android.ext.android.getKoin
import org.koin.dsl.module

class MainActivity : ComponentActivity() {
    private val mediaRequest = AndroidMediaRequest(this)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _: Boolean ->
    }
    private val launchNavigationRequestState = mutableStateOf(LaunchNavigationRequest.none())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        getKoin().loadModules(
            listOf(
                module {
                    factory<MediaRequest> { mediaRequest }
                },
            ),
        )

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        launchNavigationRequestState.value = createLaunchNavigationRequest(intent)

        setContent {
            App(
                launchNavigationRequest = launchNavigationRequestState.value,
                providePlatformRequest = { getKoin().get() },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchNavigationRequestState.value = createLaunchNavigationRequest(intent)
    }

    private fun createLaunchNavigationRequest(intent: Intent): LaunchNavigationRequest {
        val navigator = when {
            intent.getBooleanExtra(EXTRA_OPEN_LOCAL_MODEL_SETTINGS, false) -> Navigator.Settings
            else -> getChatRoomIdFromIntent(intent)?.let { chatRoomId ->
                Navigator.Chat(
                    openContext = Navigator.Chat.ChatOpenContext.OpenChat(chatRoomId),
                )
            }
        }
        return LaunchNavigationRequest(
            id = System.nanoTime(),
            navigator = navigator,
        )
    }

    private fun getChatRoomIdFromIntent(intent: Intent): ChatRoomId? {
        val chatRoomIdString = intent.getStringExtra(EXTRA_CHATROOM_ID) ?: return null
        val chatRoomIdLong = chatRoomIdString.toLongOrNull() ?: return null
        return ChatRoomId(chatRoomIdLong)
    }
}
