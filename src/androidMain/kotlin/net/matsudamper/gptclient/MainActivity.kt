package net.matsudamper.gptclient

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
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

        platformRequest.createNotificationChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            LaunchedEffect(intent) {
                val chatRoomId = intent?.getStringExtra("chatRoomId")
                if (chatRoomId != null) {
                    platformRequest.handleNotificationLaunch(chatRoomId)
                }
            }

            App()
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val chatRoomId = intent.getStringExtra("chatRoomId")
        if (chatRoomId != null) {
            platformRequest.handleNotificationLaunch(chatRoomId)
        }
    }
}
