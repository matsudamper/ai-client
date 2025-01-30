package net.matsudamper.gptclient.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.room.useWriterConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.matsudamper.gptclient.PlatformRequest
import net.matsudamper.gptclient.datastore.SettingDataStore
import net.matsudamper.gptclient.gpt.ChatGptClient
import net.matsudamper.gptclient.gpt.GptResponse
import net.matsudamper.gptclient.navigation.Navigator
import net.matsudamper.gptclient.room.AppDatabase
import net.matsudamper.gptclient.room.entity.Chat
import net.matsudamper.gptclient.room.entity.ChatRoom
import net.matsudamper.gptclient.room.entity.ChatRoomId
import net.matsudamper.gptclient.ui.ChatListUiState

class ChatViewModel(
    openContext: Navigator.Chat.ChatOpenContext,
    private val platformRequest: PlatformRequest,
    private val appDatabase: AppDatabase,
    private val settingDataStore: SettingDataStore,
    private val navControllerProvider: () -> NavController,
) : ViewModel() {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())
    val uiStateFlow: StateFlow<ChatListUiState> = MutableStateFlow(
        ChatListUiState(
            items = listOf(),
            selectedMedia = listOf(),
            listener = object : ChatListUiState.Listener {
                override fun onClickImage() {
                    viewModelScope.launch {
                        val media = platformRequest.getMedia()
                        viewModelStateFlow.update {
                            it.copy(
                                selectedMedia = media,
                            )
                        }
                    }
                }

                override fun onClickVoice() {

                }

                override fun onClickSend(text: String) {
                    addRequest(text)
                }
            }
        )
    ).also { uiState ->
        viewModelScope.launch {
            when (openContext) {
                is Navigator.Chat.ChatOpenContext.NewMessage -> {
                    val room = withContext(Dispatchers.IO) {
                        val room = ChatRoom(
                            modelName = "gpt-4o-mini", // TODO SELECT
                        )
                        room.copy(
                            id = ChatRoomId(appDatabase.chatRoomDao().insert(room))
                        )
                    }

                    viewModelStateFlow.update {
                        it.copy(room = room)
                    }
                    addRequest(openContext.initialMessage)
                }

                is Navigator.Chat.ChatOpenContext.OpenChat -> {
                    restoreChatRoom(openContext.chatRoomId)
                }
            }
        }
        viewModelScope.launch {
            viewModelStateFlow.map { it.room?.id }
                .filterNotNull()
                .stateIn(this)
                .collectLatest { roomId ->
                    appDatabase.chatDao().get(chatRoomId = roomId.value)
                        .collectLatest { chats ->
                            viewModelStateFlow.update { viewModelState ->
                                viewModelState.copy(
                                    chats = chats
                                )
                            }
                        }
                }
        }
        viewModelScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                uiState.update {
                    it.copy(
                        selectedMedia = viewModelState.selectedMedia,
                        items = viewModelState.chats.mapNotNull { chat ->
                            val message = chat.textMessage ?: return@mapNotNull null
                            // TODO: it.imageMessage
                            when (chat.role) {
                                Chat.Role.System -> {
                                    ChatListUiState.Item.Agent(
                                        message = message,
                                    )
                                }

                                Chat.Role.User,
                                Chat.Role.Unknown -> {
                                    ChatListUiState.Item.User(
                                        message = message,
                                    )
                                }

                                Chat.Role.Assistant -> {
                                    ChatListUiState.Item.Agent(
                                        message = message,
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    private fun restoreChatRoom(chatRoomId: ChatRoomId) {
        viewModelScope.launch {
            val room = appDatabase.chatRoomDao().get(chatRoomId = chatRoomId.value)
                .first()
            viewModelStateFlow.update {
                it.copy(
                    room = room,
                )
            }
        }
    }

    private fun addRequest(message: String) {
        viewModelScope.launch {
            val chatRoomId = viewModelStateFlow.value.room?.id ?: return@launch
            val chatDao = appDatabase.chatDao()
            val lastItem = chatDao.getChatRoomLastIndexItem(
                chatRoomId = chatRoomId.value,
            )
            val newChatIndex = lastItem?.index?.plus(1) ?: 0

            chatDao.insertAll(
                Chat(
                    chatRoomId = chatRoomId,
                    index = newChatIndex,
                    textMessage = message,
                    imageMessage = null,
                    role = Chat.Role.User,
                )
            )

            val chats = chatDao.get(chatRoomId = chatRoomId.value)
                .first()

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
                    if (it.imageMessage != null) {
                        TODO()
                    }
                }


                ChatGptClient.GptMessage(
                    role = role,
                    contents = contents
                )
            }
            val response = getGptClient().request(
                messages = messages,
            )
            val roomChats = response.choices.mapIndexed { index, choice ->
                Chat(
                    chatRoomId = chatRoomId,
                    index = newChatIndex + 1 + index,
                    textMessage = choice.message.content,
                    imageMessage = null, // TODO
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
    }

    private suspend fun getGptClient(): ChatGptClient {
        return ChatGptClient(settingDataStore.getSecretKey())
    }

    private data class ViewModelState(
        val room: ChatRoom? = null,
        val chats: List<Chat> = listOf(),
        val selectedMedia: List<String> = listOf(),
    )
}