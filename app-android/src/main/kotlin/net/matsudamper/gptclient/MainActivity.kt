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
import androidx.core.view.WindowCompat
import net.matsudamper.gptclient.room.entity.ChatRoomId
import org.koin.android.ext.android.getKoin
import org.koin.dsl.module

class MainActivity : ComponentActivity() {
    private val platformRequest = AndroidPlatformRequest(
        activity = this,
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = true
        getKoin().loadModules(
            listOf(
                module {
                    factory<PlatformRequest> { platformRequest }
                },
            ),
        )

        platformRequest.createNotificationChannel(GPT_CLIENT_NOTIFICATION_ID)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val chatRoomId = getChatRoomIdFromIntent(intent)

        setContent {
            App(initialChatRoomId = chatRoomId)
        }
    }

    private fun getChatRoomIdFromIntent(intent: Intent): ChatRoomId? {
        val chatRoomIdString = intent.getStringExtra(KEY_CHATROOM_ID) ?: return null
        val chatRoomIdLong = chatRoomIdString.toLongOrNull() ?: return null
        return ChatRoomId(chatRoomIdLong)
    }

    companion object {
        const val KEY_CHATROOM_ID = "chatRoomId"
        const val GPT_CLIENT_NOTIFICATION_ID = "gpt_client_notifications"
    }
}
