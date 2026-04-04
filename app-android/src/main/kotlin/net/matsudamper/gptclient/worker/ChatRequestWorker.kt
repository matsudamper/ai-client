package net.matsudamper.gptclient.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import kotlin.random.Random
import kotlinx.coroutines.flow.first
import net.matsudamper.gptclient.EXTRA_CHATROOM_ID
import net.matsudamper.gptclient.GPT_CLIENT_NOTIFICATION_CHANNEL_ID
import net.matsudamper.gptclient.MainActivity
import net.matsudamper.gptclient.PlatformRequest
import net.matsudamper.gptclient.localmodel.LocalModelClientFactory
import net.matsudamper.gptclient.datastore.SettingDataStore
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
    private val localModelClientFactory: LocalModelClientFactory = GlobalContext.get().get()

    override suspend fun doWork(): Result {
        val chatRoomId = ChatRoomId(inputData.getLong(KEY_CHAT_ROOM_ID, 0))
        val message = inputData.getString(KEY_MESSAGE).orEmpty()
        val uris = inputData.getStringArray(KEY_URIS)?.toList().orEmpty()

        val chatRoom = appDatabase.chatRoomDao()

        val firstChatRoom = chatRoom.get(chatRoomId = chatRoomId.value).first()
        val roomTitle = firstChatRoom.summary ?: "チャット"

        val pendingIntent = createPendingIntent(chatRoomId = chatRoomId.value.toString())
        setForeground(
            ForegroundInfo(
                Random.nextInt(),
                createNotificationBuilder(
                    title = roomTitle,
                    message = "処理中...",
                    channelId = GPT_CLIENT_NOTIFICATION_CHANNEL_ID,
                    pendingIntent = pendingIntent,
                )
                    .setOngoing(true)
                    .setProgress(1, 1, true).build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            ),
        )

        return when (
            val result = ChatRequestRunner(
                appDatabase = appDatabase,
                platformRequest = platformRequest,
                settingDataStore = settingDataStore,
                localModelRepository = localModelRepository,
                localModelClientFactory = localModelClientFactory,
            ).run(
                chatRoomId = chatRoomId,
                message = message,
                uris = uris,
            )
        ) {
            is ChatRequestRunner.Result.Error -> {
                snowFinishNotification(
                    title = "処理失敗",
                    message = result.errorMessage,
                    channelId = GPT_CLIENT_NOTIFICATION_CHANNEL_ID,
                    notificationId = Random.nextInt(),
                    pendingIntent = pendingIntent,
                )
                Result.failure()
            }

            ChatRequestRunner.Result.Success -> {
                val updatedRoom = appDatabase.chatRoomDao().get(chatRoomId = chatRoomId.value).first()
                val notificationTitle = updatedRoom.summary ?: roomTitle

                snowFinishNotification(
                    title = "処理完了",
                    message = "${notificationTitle}の処理が完了しました",
                    channelId = GPT_CLIENT_NOTIFICATION_CHANNEL_ID,
                    notificationId = Random.nextInt(),
                    pendingIntent = pendingIntent,
                )
                Result.success()
            }
        }
    }

    private fun snowFinishNotification(
        title: String,
        message: String,
        channelId: String,
        notificationId: Int,
        pendingIntent: PendingIntent,
    ) {
        val builder = createNotificationBuilder(
            title = title,
            message = message,
            channelId = channelId,
            pendingIntent = pendingIntent,
        )

        if (android.content.pm.PackageManager.PERMISSION_GRANTED ==
            ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.POST_NOTIFICATIONS,
            )
        ) {
            NotificationManagerCompat.from(applicationContext)
                .notify(notificationId, builder.build())
        }
    }

    private fun createPendingIntent(
        chatRoomId: String?,
    ): PendingIntent {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (chatRoomId != null) {
                putExtra(EXTRA_CHATROOM_ID, chatRoomId)
            }
        }
        return PendingIntent.getActivity(
            applicationContext,
            chatRoomId?.hashCode() ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createNotificationBuilder(
        title: String,
        message: String,
        channelId: String,
        pendingIntent: PendingIntent,
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
    }

    companion object {
        const val KEY_CHAT_ROOM_ID = "chat_room_id"
        const val KEY_MESSAGE = "message"
        const val KEY_URIS = "uris"

        fun createInputData(
            chatRoomId: ChatRoomId,
            message: String,
            uris: List<String>,
        ): Data {
            return Data.Builder()
                .putLong(KEY_CHAT_ROOM_ID, chatRoomId.value)
                .putString(KEY_MESSAGE, message)
                .putStringArray(KEY_URIS, uris.toTypedArray())
                .build()
        }
    }
}
