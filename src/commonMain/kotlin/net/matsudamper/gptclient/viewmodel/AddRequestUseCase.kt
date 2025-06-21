package net.matsudamper.gptclient.viewmodel

import androidx.room.useWriterConnection
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import net.matsudamper.gptclient.PlatformRequest
import net.matsudamper.gptclient.datastore.SettingDataStore
import net.matsudamper.gptclient.entity.ChatGptModel
import net.matsudamper.gptclient.gpt.ChatGptClient
import net.matsudamper.gptclient.gpt.GptResponse
import net.matsudamper.gptclient.room.AppDatabase
import net.matsudamper.gptclient.room.entity.Chat
import net.matsudamper.gptclient.room.entity.ChatRoomId

class AddRequestUseCase(
    private val appDatabase: AppDatabase,
    private val platformRequest: PlatformRequest,
    private val gptClientProvider: (secretKey: String) -> ChatGptClient,
    private val settingDataStore: SettingDataStore,
) {
    suspend fun add(
        chatRoomId: ChatRoomId,
        message: String,
        uris: List<String>,
        format: ChatGptClient.Format,
        systemMessage: String?,
        model: String,
        summaryProvider: (String) -> String?,
    ): Result {
        @Suppress("OPT_IN_USAGE")
        return GlobalScope.async async@{
            if (message.isEmpty() && uris.isEmpty()) return@async Result.InputError
            val chatDao = appDatabase.chatDao()
            val lastItem = chatDao.getChatRoomLastIndexItem(
                chatRoomId = chatRoomId.value,
            )
            val newChatIndex = lastItem?.index?.plus(1) ?: 0
            insertNewMessage(
                message = message,
                uris = uris,
                newChatIndex = newChatIndex,
                chatRoomId = chatRoomId,
            )

            val response = when (
                val response = gptClientProvider(settingDataStore.getSecretKey())
                    .request(
                        messages = createMessage(
                            systemMessage = systemMessage,
                            chatRoomId = chatRoomId,
                        ),
                        format = format,
                        model = ChatGptModel.entries.firstOrNull { it.modelName == model }
                            ?: return@async Result.ModelNotFoundError,
                    )
            ) {
                is ChatGptClient.GptResult.Error -> return@async Result.GptResultError(response)
                is ChatGptClient.GptResult.Success -> response.response
            }
            val roomChats = response.choices.mapIndexed { index, choice ->
                Chat(
                    chatRoomId = chatRoomId,
                    index = newChatIndex + 1 + index,
                    textMessage = choice.message.content,
                    imageUri = null,
                    role = when (choice.message.role) {
                        GptResponse.Choice.Role.System -> {
                            Chat.Role.System
                        }

                        GptResponse.Choice.Role.User -> {
                            Chat.Role.User
                        }

                        GptResponse.Choice.Role.Assistant -> {
                            Chat.Role.Assistant
                        }

                        null -> Chat.Role.User
                    },
                )
            }

            appDatabase.useWriterConnection {
                appDatabase.chatDao().apply {
                    insertAll(*roomChats.toTypedArray())
                }
            }
            writeSummary(
                chatRoomId = chatRoomId,
                response = response,
                summaryProvider = summaryProvider,
            )

            return@async Result.Success
        }.await()
    }

    private suspend fun writeSummary(
        chatRoomId: ChatRoomId,
        response: GptResponse,
        summaryProvider: (String) -> String?,
    ) {
        val chatRoomDao = appDatabase.chatRoomDao()
        chatRoomDao.get(chatRoomId = chatRoomId.value).first().let { room ->
            if (room.summary == null) {
                val message = response.choices
                    .firstOrNull { it.message.role == GptResponse.Choice.Role.Assistant }
                    ?.message ?: return@let

                val summary = summaryProvider(message.content) ?: return@let
                chatRoomDao.update(
                    room.copy(summary = summary),
                )
            }
        }
    }

    private suspend fun insertNewMessage(
        message: String,
        uris: List<String>,
        newChatIndex: Int,
        chatRoomId: ChatRoomId,
    ) {
        val chatDao = appDatabase.chatDao()
        chatDao.insertAll(
            uris
                .map {
                    Chat(
                        chatRoomId = chatRoomId,
                        index = newChatIndex,
                        textMessage = null,
                        imageUri = it,
                        role = Chat.Role.User,
                    )
                },
        )
        if (message.isNotEmpty()) {
            chatDao.insertAll(
                Chat(
                    chatRoomId = chatRoomId,
                    index = newChatIndex,
                    textMessage = message,
                    imageUri = null,
                    role = Chat.Role.User,
                ),
            )
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

    sealed interface Result {
        data object Success : Result
        data class GptResultError(val gptError: ChatGptClient.GptResult.Error) : Result
        data object InputError : Result
        data object ModelNotFoundError : Result
    }
}
