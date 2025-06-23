package net.matsudamper.gptclient.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.flow.first
import net.matsudamper.gptclient.PlatformRequest
import net.matsudamper.gptclient.datastore.SettingDataStore
import net.matsudamper.gptclient.entity.ChatGptModel
import net.matsudamper.gptclient.gpt.ChatGptClient
import net.matsudamper.gptclient.gpt.GptResponse
import net.matsudamper.gptclient.room.AppDatabase
import net.matsudamper.gptclient.room.entity.Chat
import net.matsudamper.gptclient.room.entity.ChatRoomId
import net.matsudamper.gptclient.viewmodel.GetBuiltinProjectInfoUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
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

        val room = appDatabase.chatRoomDao().get(chatRoomId = chatRoomId.value).first()
        val roomTitle = room.summary ?: "チャット"

        val (format, systemMessage, model) = when (val builtinProjectId = room.builtInProjectId) {
            null -> Triple(
                ChatGptClient.Format.Text,
                null,
                room.modelName,
            )

            else -> {
                val builtinProjectInfo = GetBuiltinProjectInfoUseCase().exec(
                    builtinProjectId = builtinProjectId,
                    platformRequest = platformRequest,
                )
                Triple(
                    builtinProjectInfo.format,
                    builtinProjectInfo.systemMessage,
                    builtinProjectInfo.model.modelName,
                )
            }
        }

        try {
            if (message.isEmpty() && uris.isEmpty()) return Result.failure()

            val chatDao = appDatabase.chatDao()
            val lastItem = chatDao.getChatRoomLastIndexItem(
                chatRoomId = chatRoomId.value,
            )
            val newChatIndex = lastItem?.index?.plus(1) ?: 0

            val gptClientProvider: (String) -> ChatGptClient = { secretKey ->
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
                is ChatGptClient.GptResult.Error -> return Result.failure()
                is ChatGptClient.GptResult.Success -> response.response
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

            appDatabase.chatDao().insertAll(*roomChats.toTypedArray())

            writeSummary(chatRoomId = chatRoomId, response = response)

            val chatRoomDao = appDatabase.chatRoomDao()
            chatRoomDao.update(room.copy(workerId = null))

            val updatedRoom = appDatabase.chatRoomDao().get(chatRoomId = chatRoomId.value).first()
            val notificationTitle = updatedRoom.summary ?: roomTitle
            platformRequest.showNotification(
                title = "処理完了",
                message = "${notificationTitle}の処理が完了しました",
                chatRoomId = chatRoomId.value.toString(),
            )

            return Result.success()
        } catch (_: Throwable) {
            val chatRoomDao = appDatabase.chatRoomDao()
            chatRoomDao.update(room.copy(workerId = null))
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
    ): List<ChatGptClient.GptMessage> {
        val chatDao = appDatabase.chatDao()
        val chats = chatDao.get(chatRoomId = chatRoomId.value)
            .first()

        val systemMessage = run {
            systemMessage ?: return@run null
            ChatGptClient.GptMessage(
                role = ChatGptClient.GptMessage.Role.System,
                contents = listOf(ChatGptClient.GptMessage.Content.Text(systemMessage)),
            )
        }
        val messages = chats.map {
            val role = when (it.role) {
                Chat.Role.System -> ChatGptClient.GptMessage.Role.System
                Chat.Role.User -> ChatGptClient.GptMessage.Role.User
                Chat.Role.Assistant -> ChatGptClient.GptMessage.Role.Assistant
                Chat.Role.Unknown -> ChatGptClient.GptMessage.Role.User
            }
            val contents = buildList {
                val textMessage = it.textMessage
                if (textMessage != null) {
                    add(ChatGptClient.GptMessage.Content.Text(textMessage))
                }
                val imageMessage = it.imageUri
                if (imageMessage != null) {
                    val byteArray = platformRequest.readPngByteArray(uri = imageMessage)
                    if (byteArray == null) {
                        return listOf()
                    }
                    add(
                        ChatGptClient.GptMessage.Content.Base64Image(
                            @OptIn(ExperimentalEncodingApi::class)
                            Base64.encode(byteArray),
                        ),
                    )
                }
            }

            ChatGptClient.GptMessage(
                role = role,
                contents = contents,
            )
        }
        return buildList {
            add(systemMessage)
            addAll(messages)
        }.filterNotNull()
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
