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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.lastOrNull
import net.matsudamper.gptclient.MainActivity
import net.matsudamper.gptclient.PlatformRequest
import net.matsudamper.gptclient.datastore.SettingDataStore
import net.matsudamper.gptclient.entity.ApiProvider
import net.matsudamper.gptclient.entity.ChatGptModel
import net.matsudamper.gptclient.client.openai.ChatGptClient
import net.matsudamper.gptclient.client.AiClient
import net.matsudamper.gptclient.client.gemini.GeminiClient
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

        val firstChatRoom = chatRoom.get(chatRoomId = chatRoomId.value).first()
        val roomTitle = firstChatRoom.summary ?: "チャット"

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

        val format: AiClient.Format
        val systemMessage: String?
        val model: String
        when (val builtinProjectId = firstChatRoom.builtInProjectId) {
            null -> when (val projectId = firstChatRoom.projectId) {
                null -> {
                    format = AiClient.Format.Text
                    systemMessage = null
                    model = firstChatRoom.modelName
                }
                else -> {
                    val projectDao = appDatabase.projectDao()
                    val project = projectDao.get(projectId.id).first()
                    format = AiClient.Format.Text
                    systemMessage = project?.systemMessage
                    model = firstChatRoom.modelName
                }
            }

            else -> {
                val builtinProjectInfo = GetBuiltinProjectInfoUseCase().exec(
                    builtinProjectId = builtinProjectId,
                    platformRequest = platformRequest,
                )
                format = builtinProjectInfo.format
                systemMessage = builtinProjectInfo.systemMessage
                model = firstChatRoom.modelName
            }
        }

        try {
            if (message.isEmpty() && uris.isEmpty()) return Result.failure()

            val lastItem = chatDao.getChatRoomLastIndexItem(
                chatRoomId = chatRoomId.value,
            )
            val newChatIndex = lastItem?.index?.plus(1) ?: 0

            val chatModel = ChatGptModel.entries.firstOrNull { it.modelName == model }
                ?: return Result.failure()

            val gptClient: AiClient = when (chatModel.provider) {
                ApiProvider.OpenAI -> ChatGptClient(
                    secretKey = settingDataStore.getSecretKey(),
                )
                ApiProvider.Gemini -> GeminiClient(
                    apiKey = if (chatModel.requireBillingKey) {
                        settingDataStore.getGeminiBillingKey()
                    } else {
                        settingDataStore.getGeminiSecretKey()
                    },
                )
            }

            val response = when (
                val response = gptClient
                    .request(
                        messages = createMessage(
                            systemMessage = systemMessage,
                            chatRoomId = chatRoomId,
                        ),
                        format = format,
                        model = chatModel,
                    )
            ) {
                is AiClient.GptResult.Error -> {
                    chatRoomDao.update(id = chatRoomId) {
                        it.copy(
                            workerId = null,
                            latestErrorMessage = response.reason.message
                        )
                    }

                    snowFinishNotification(
                        title = "処理失敗",
                        message = response.reason.message,
                        channelId = MainActivity.GPT_CLIENT_NOTIFICATION_ID,
                        notificationId = Random.nextInt(),
                        pendingIntent = pendingIntent,
                    )
                    return Result.failure()
                }
                is AiClient.GptResult.Success -> response.response
            }

            val roomChats = response.choices.mapIndexed { index, choice ->
                Chat(
                    chatRoomId = chatRoomId,
                    index = newChatIndex + 1 + index,
                    textMessage = choice.message.content,
                    imageUri = null,
                    role = when (choice.message.role) {
                        AiClient.AiResponse.Choice.Role.System -> Chat.Role.System
                        AiClient.AiResponse.Choice.Role.User -> Chat.Role.User
                        AiClient.AiResponse.Choice.Role.Assistant -> Chat.Role.Assistant
                        null -> Chat.Role.User
                    },
                )
            }

            chatDao.insertAll(*roomChats.toTypedArray())

            writeSummary(chatRoomId = chatRoomId, response = response)

            chatRoomDao.update(id = chatRoomId) {
                it.copy(
                    workerId = null,
                )
            }

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
            e.printStackTrace()
            val chatRoomDao = appDatabase.chatRoomDao()

            chatRoomDao.update(id = chatRoomId) {
                it.copy(
                    workerId = null,
                )
            }

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
        response: AiClient.AiResponse,
    ) {
        val chatRoomDao = appDatabase.chatRoomDao()
        val chatDao = appDatabase.chatDao()
        val room = chatRoomDao.get(chatRoomId = chatRoomId.value).first()
        val firstInstruction = chatDao.get(chatRoomId.value)
            .firstOrNull()
            ?.firstOrNull { it.role == Chat.Role.User }
            ?.textMessage
            ?.takeIf { it.isNotBlank() }
        val lastInstruction = chatDao.get(chatRoomId.value)
            .lastOrNull()
            ?.firstOrNull { it.role == Chat.Role.User }
            ?.textMessage
            ?.takeIf { it.isNotBlank() }
        val message = response.choices
            .lastOrNull { it.message.role == AiClient.AiResponse.Choice.Role.Assistant }
            ?.message ?: return

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
                builtinProjectInfo.summaryProvider.provide(firstInstruction, lastInstruction,message.content)
            }
        }

        if (summary != null) {
            chatRoomDao.update(
                room.copy(summary = summary),
            )
        }
    }

    private suspend fun createMessage(
        systemMessage: String?,
        chatRoomId: ChatRoomId,
    ): List<AiClient.GptMessage> {
        val chatDao = appDatabase.chatDao()
        val chats = chatDao.get(chatRoomId = chatRoomId.value)
            .first()

        val systemMessage = run {
            systemMessage ?: return@run null
            AiClient.GptMessage(
                role = AiClient.GptMessage.Role.System,
                contents = listOf(AiClient.GptMessage.Content.Text(systemMessage)),
            )
        }
        val messages = chats.map {
            val role = when (it.role) {
                Chat.Role.System -> AiClient.GptMessage.Role.System
                Chat.Role.User -> AiClient.GptMessage.Role.User
                Chat.Role.Assistant -> AiClient.GptMessage.Role.Assistant
                Chat.Role.Unknown -> AiClient.GptMessage.Role.User
            }
            val contents = buildList {
                val textMessage = it.textMessage
                if (textMessage != null) {
                    add(AiClient.GptMessage.Content.Text(textMessage))
                }
                val imageMessage = it.imageUri
                if (imageMessage != null) {
                    val byteArray = platformRequest.readPngByteArray(uri = imageMessage)
                    if (byteArray == null) {
                        return listOf()
                    }
                    add(
                        AiClient.GptMessage.Content.Base64Image(
                            @OptIn(ExperimentalEncodingApi::class)
                            Base64.encode(byteArray),
                        ),
                    )
                }
            }

            AiClient.GptMessage(
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
