package net.matsudamper.gptclient

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import net.matsudamper.gptclient.localmodel.EXTRA_OPEN_LOCAL_MODEL_SETTINGS
import net.matsudamper.gptclient.navigation.Navigator
import net.matsudamper.gptclient.room.entity.BuiltinProjectId
import net.matsudamper.gptclient.room.entity.ChatRoomId
import net.matsudamper.gptclient.room.entity.ProjectId
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
            else -> getProjectNavigatorFromIntent(intent)
                ?: getChatRoomIdFromIntent(intent)?.let { chatRoomId ->
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

    private fun getProjectNavigatorFromIntent(intent: Intent): Navigator.Project? {
        val title = intent.getStringExtra(EXTRA_SHORTCUT_PROJECT_TITLE) ?: return null
        val builtinProjectId = intent.getStringExtra(EXTRA_SHORTCUT_BUILTIN_PROJECT_ID)
        if (builtinProjectId != null) {
            return Navigator.Project(
                title = title,
                type = Navigator.Project.ProjectType.Builtin(BuiltinProjectId(builtinProjectId)),
            )
        }
        val projectId = intent.getLongExtra(EXTRA_SHORTCUT_PROJECT_ID, -1L)
        if (projectId >= 0L) {
            return Navigator.Project(
                title = title,
                type = Navigator.Project.ProjectType.Project(ProjectId(projectId)),
            )
        }
        return null
    }

    private fun getChatRoomIdFromIntent(intent: Intent): ChatRoomId? {
        val chatRoomIdString = intent.getStringExtra(EXTRA_CHATROOM_ID) ?: return null
        val chatRoomIdLong = chatRoomIdString.toLongOrNull() ?: return null
        return ChatRoomId(chatRoomIdLong)
    }
}
