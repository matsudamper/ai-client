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
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random
import kotlinx.coroutines.flow.first
import net.matsudamper.gptclient.MainActivity
import net.matsudamper.gptclient.PlatformRequest
import net.matsudamper.gptclient.datastore.SettingDataStore
import net.matsudamper.gptclient.entity.ChatGptModel
import net.matsudamper.gptclient.gpt.ChatGptClient
import net.matsudamper.gptclient.gpt.ChatGptClientInterface
import net.matsudamper.gptclient.gpt.GptResponse
import net.matsudamper.gptclient.room.AppDatabase
import net.matsudamper.gptclient.room.entity.Chat
import net.matsudamper.gptclient.room.entity.ChatRoomId
import net.matsudamper.gptclient.viewmodel.GetBuiltinProjectInfoUseCase
import org.koin.core.context.GlobalContext

class ChatRequestWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    private val appDatabase: AppDatabase = GlobalContext.get().get()
    private val platformRequest: PlatformRequest = GlobalContext.get().get()
    private val settingDataStore: SettingDataStore = GlobalContext.get().get()

    override suspend fun doWork(): Result {
        val chatRoomId = ChatRoomId(inputData.getLong(KEY_CHAT_ROOM_ID, 0))
        val message = inputData.getString(KEY_MESSAGE).orEmpty()
        val uris = inputData.getStringArray(KEY_URIS)?.toList().orEmpty()

        val chatRoomDao = appDatabase.chatRoomDao()
        val chatDao = appDatabase.chatDao()
        val chatRoom = appDatabase.chatRoomDao()

        val room = chatRoom.get(chatRoomId = chatRoomId.value).first()
        val roomTitle = room.summary ?: "チャット"

        val pendingIntent = createPendingIntent(chatRoomId = chatRoomId.value.toString())
        setForeground(
            ForegroundInfo(
                Random.nextInt(),
                createNotificationBuilder(
                    title = roomTitle,
                    message = "処理中...",
                    channelId = MainActivity.GPT_CLIENT_NOTIFICATION_ID,
                    pendingIntent = pendingIntent,
                )
                    .setOngoing(true)
                    .setProgress(1, 1, true).build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            ),
        )

        val format: ChatGptClientInterface.Format
        val systemMessage: String?
        val model: String
        when (val builtinProjectId = room.builtInProjectId) {
            null -> when (val projectId = room.projectId) {
                null -> throw IllegalStateException("Project Not Found.")
                else -> {
                    val projectDao = appDatabase.projectDao()
                    val project = projectDao.get(projectId.id).first()
                    format = ChatGptClientInterface.Format.Text
                    systemMessage = project?.systemMessage
                    model = room.modelName
                }
            }

            else -> {
                val builtinProjectInfo = GetBuiltinProjectInfoUseCase().exec(
                    builtinProjectId = builtinProjectId,
                    platformRequest = platformRequest,
                )
                format = builtinProjectInfo.format
                systemMessage = builtinProjectInfo.systemMessage
                model = builtinProjectInfo.model.modelName
            }
        }

        try {
            if (message.isEmpty() && uris.isEmpty()) return Result.failure()

            val lastItem = chatDao.getChatRoomLastIndexItem(
                chatRoomId = chatRoomId.value,
            )
            val newChatIndex = lastItem?.index?.plus(1) ?: 0

            val gptClientProvider: (String) -> ChatGptClientInterface = { secretKey ->
                ChatGptClient(secretKey)
            }

            val response = when (
                val response = gptClientProvider(settingDataStore.getSecretKey())
                    .request(
                        messages = createMessage(
                            systemMessage = systemMessage,
                            chatRoomId = chatRoomId,
                        ),
                        format = format,
                        model = ChatGptModel.entries.firstOrNull { it.modelName == model }
                            ?: return Result.failure(),
                    )
            ) {
                is ChatGptClientInterface.GptResult.Error -> return Result.failure()
                is ChatGptClientInterface.GptResult.Success -> response.response
            }

            val roomChats = response.choices.mapIndexed { index, choice ->
                Chat(
                    chatRoomId = chatRoomId,
                    index = newChatIndex + 1 + index,
                    textMessage = choice.message.content,
                    imageUri = null,
                    role = when (choice.message.role) {
                        GptResponse.Choice.Role.System -> Chat.Role.System
                        GptResponse.Choice.Role.User -> Chat.Role.User
                        GptResponse.Choice.Role.Assistant -> Chat.Role.Assistant
                        null -> Chat.Role.User
                    },
                )
            }

            chatDao.insertAll(*roomChats.toTypedArray())

            writeSummary(chatRoomId = chatRoomId, response = response)

            chatRoomDao.update(room.copy(workerId = null))

            val updatedRoom = appDatabase.chatRoomDao().get(chatRoomId = chatRoomId.value).first()
            val notificationTitle = updatedRoom.summary ?: roomTitle

            snowFinishNotification(
                title = "処理完了",
                message = "${notificationTitle}の処理が完了しました",
                channelId = MainActivity.GPT_CLIENT_NOTIFICATION_ID,
                notificationId = Random.nextInt(),
                pendingIntent = pendingIntent,
            )

            return Result.success()
        } catch (e: Throwable) {
            val chatRoomDao = appDatabase.chatRoomDao()
            chatRoomDao.update(room.copy(workerId = null))
            snowFinishNotification(
                title = "処理失敗",
                message = e.message.orEmpty(),
                channelId = MainActivity.GPT_CLIENT_NOTIFICATION_ID,
                notificationId = Random.nextInt(),
                pendingIntent = pendingIntent,
            )
            return Result.failure()
        }
    }

    private suspend fun writeSummary(
        chatRoomId: ChatRoomId,
        response: GptResponse,
    ) {
        val chatRoomDao = appDatabase.chatRoomDao()
        chatRoomDao.get(chatRoomId = chatRoomId.value).first().let { room ->
            if (room.summary == null) {
                val message = response.choices
                    .firstOrNull { it.message.role == GptResponse.Choice.Role.Assistant }
                    ?.message ?: return@let

                val summary = when (val builtinProjectId = room.builtInProjectId) {
                    null -> {
                        message.content.take(50).takeIf { it.isNotBlank() }?.let { summary ->
                            if (summary.length == 50 && message.content.length > 50) "$summary..." else summary
                        }
                    }

                    else -> {
                        val builtinProjectInfo = GetBuiltinProjectInfoUseCase().exec(
                            builtinProjectId = builtinProjectId,
                            platformRequest = platformRequest,
                        )
                        builtinProjectInfo.summaryProvider(message.content)
                    }
                }

                if (summary != null) {
                    chatRoomDao.update(
                        room.copy(summary = summary),
                    )
                }
            }
        }
    }

    private suspend fun createMessage(
        systemMessage: String?,
        chatRoomId: ChatRoomId,
    ): List<ChatGptClientInterface.GptMessage> {
        val chatDao = appDatabase.chatDao()
        val chats = chatDao.get(chatRoomId = chatRoomId.value)
            .first()

        val systemMessage = run {
            systemMessage ?: return@run null
            ChatGptClientInterface.GptMessage(
                role = ChatGptClientInterface.GptMessage.Role.System,
                contents = listOf(ChatGptClientInterface.GptMessage.Content.Text(systemMessage)),
            )
        }
        val messages = chats.map {
            val role = when (it.role) {
                Chat.Role.System -> ChatGptClientInterface.GptMessage.Role.System
                Chat.Role.User -> ChatGptClientInterface.GptMessage.Role.User
                Chat.Role.Assistant -> ChatGptClientInterface.GptMessage.Role.Assistant
                Chat.Role.Unknown -> ChatGptClientInterface.GptMessage.Role.User
            }
            val contents = buildList {
                val textMessage = it.textMessage
                if (textMessage != null) {
                    add(ChatGptClientInterface.GptMessage.Content.Text(textMessage))
                }
                val imageMessage = it.imageUri
                if (imageMessage != null) {
                    val byteArray = platformRequest.readPngByteArray(uri = imageMessage)
                    if (byteArray == null) {
                        return listOf()
                    }
                    add(
                        ChatGptClientInterface.GptMessage.Content.Base64Image(
                            @OptIn(ExperimentalEncodingApi::class)
                            Base64.encode(byteArray),
                        ),
                    )
                }
            }

            ChatGptClientInterface.GptMessage(
                role = role,
                contents = contents,
            )
        }
        return buildList {
            add(systemMessage)
            addAll(messages)
        }.filterNotNull()
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
                putExtra(MainActivity.KEY_CHATROOM_ID, chatRoomId)
            }
        }
        return PendingIntent.getActivity(
            applicationContext,
            0,
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
