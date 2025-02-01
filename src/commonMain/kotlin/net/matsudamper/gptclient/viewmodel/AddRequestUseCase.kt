package net.matsudamper.gptclient.viewmodel

import androidx.room.useWriterConnection
import kotlinx.coroutines.flow.first
import net.matsudamper.gptclient.PlatformRequest
import net.matsudamper.gptclient.datastore.SettingDataStore
import net.matsudamper.gptclient.gpt.ChatGptClient
import net.matsudamper.gptclient.gpt.GptResponse
import net.matsudamper.gptclient.room.AppDatabase
import net.matsudamper.gptclient.room.entity.Chat
import net.matsudamper.gptclient.room.entity.ChatRoomId
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

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
    ) {
        if (message.isEmpty() && uris.isEmpty()) return
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

        val response = gptClientProvider(settingDataStore.getSecretKey()).request(
            messages = createMessage(
                systemMessage = systemMessage,
                chatRoomId = chatRoomId,
            ),
            format = format,
        )
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
                }
            )
        }

        appDatabase.useWriterConnection {
            appDatabase.chatDao().apply {
                insertAll(*roomChats.toTypedArray())
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
                }
        )
        if (message.isNotEmpty()) {
            chatDao.insertAll(
                Chat(
                    chatRoomId = chatRoomId,
                    index = newChatIndex,
                    textMessage = message,
                    imageUri = null,
                    role = Chat.Role.User,
                )
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
                contents = listOf(ChatGptClient.GptMessage.Content.Text(systemMessage))
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
                    byteArray!!
                    add(
                        ChatGptClient.GptMessage.Content.Base64Image(
                            @OptIn(ExperimentalEncodingApi::class)
                            Base64.encode(byteArray)
                        )
                    )
                }
            }

            ChatGptClient.GptMessage(
                role = role,
                contents = contents
            )
        }
        return buildList {
            add(systemMessage)
            addAll(messages)
        }.filterNotNull()
    }
}
