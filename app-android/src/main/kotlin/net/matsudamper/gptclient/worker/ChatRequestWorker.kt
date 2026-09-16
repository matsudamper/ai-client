package net.matsudamper.gptclient.worker

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlin.random.Random
import kotlinx.coroutines.flow.first
import net.matsudamper.gptclient.EXTRA_CHATROOM_ID
import net.matsudamper.gptclient.GPT_CLIENT_NOTIFICATION_CHANNEL_ID
import net.matsudamper.gptclient.MainActivity
import net.matsudamper.gptclient.PlatformRequest
import net.matsudamper.gptclient.datastore.SettingDataStore
import net.matsudamper.gptclient.localmodel.LocalModelAiClientFactory
import net.matsudamper.gptclient.localmodel.LocalModelRepository
import net.matsudamper.gptclient.room.AppDatabase
import net.matsudamper.gptclient.room.entity.ChatRoomId
import org.koin.core.context.GlobalContext

class ChatRequestWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    private val appDatabase: AppDatabase = GlobalContext.get().get()
    private val platformRequest: PlatformRequest = GlobalContext.get().get()
    private val settingDataStore: SettingDataStore = GlobalContext.get().get()
    private val localModelRepository: LocalModelRepository = GlobalContext.get().get()
    private val localModelAiClientFactory: LocalModelAiClientFactory = GlobalContext.get().get()

    override suspend fun doWork(): Result {
        val chatRoomId = ChatRoomId(inputData.getLong(KEY_CHAT_ROOM_ID, 0))
        if (runAttemptCount > 0) return Result.failure()

        val room = appDatabase.chatRoomDao().get(chatRoomId = chatRoomId.value).first()
        val roomTitle = room.summary ?: "チャット"
        val pendingIntent = createPendingIntent(chatRoomId = chatRoomId.value.toString())
        setForeground(createProgressForegroundInfo(title = roomTitle, pendingIntent = pendingIntent))

        val result = ChatRequestRunner(
            appDatabase = appDatabase,
            platformRequest = platformRequest,
            settingDataStore = settingDataStore,
            localModelRepository = localModelRepository,
            localModelAiClientFactory = localModelAiClientFactory,
        ).run(chatRoomId = chatRoomId)

        return when (result) {
            is ChatRequestRunner.Result.Error -> {
                showFinishNotification(
                    title = "処理失敗",
                    message = result.errorMessage,
                    pendingIntent = pendingIntent,
                )
                Result.failure()
            }

            ChatRequestRunner.Result.Success -> {
                val finishedRoom = appDatabase.chatRoomDao().get(chatRoomId = chatRoomId.value).first()
                showFinishNotification(
                    title = "処理完了",
                    message = "${finishedRoom.summary ?: roomTitle}の処理が完了しました",
                    pendingIntent = pendingIntent,
                )
                Result.success()
            }
        }
    }

    private fun createProgressForegroundInfo(
        title: String,
        pendingIntent: PendingIntent,
    ): ForegroundInfo {
        val cancelPendingIntent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
        val notification = createNotificationBuilder(
            title = title,
            message = "処理中...",
            pendingIntent = pendingIntent,
        )
            .setOngoing(true)
            .setProgress(1, 1, true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "キャンセル",
                cancelPendingIntent,
            )
            .build()

        return ForegroundInfo(
            Random.nextInt(),
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    private fun showFinishNotification(
        title: String,
        message: String,
        pendingIntent: PendingIntent,
    ) {
        val isNotificationGranted = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (isNotificationGranted.not()) return

        val notification = createNotificationBuilder(
            title = title,
            message = message,
            pendingIntent = pendingIntent,
        ).build()
        NotificationManagerCompat.from(applicationContext).notify(Random.nextInt(), notification)
    }

    private fun createPendingIntent(chatRoomId: String): PendingIntent {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_CHATROOM_ID, chatRoomId)
        }
        return PendingIntent.getActivity(
            applicationContext,
            chatRoomId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createNotificationBuilder(
        title: String,
        message: String,
        pendingIntent: PendingIntent,
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(applicationContext, GPT_CLIENT_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
    }

    companion object {
        const val KEY_CHAT_ROOM_ID = "chat_room_id"

        fun createInputData(
            chatRoomId: ChatRoomId,
        ): Data {
            return Data.Builder()
                .putLong(KEY_CHAT_ROOM_ID, chatRoomId.value)
                .build()
        }
    }
}
