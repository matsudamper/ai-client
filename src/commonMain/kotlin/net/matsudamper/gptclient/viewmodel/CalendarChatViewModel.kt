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
import net.matsudamper.gptclient.room.entity.BuiltinProjectId
import net.matsudamper.gptclient.room.entity.Chat
import net.matsudamper.gptclient.room.entity.ChatRoom
import net.matsudamper.gptclient.room.entity.ChatRoomId
import net.matsudamper.gptclient.ui.ChatListUiState
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class CalendarChatViewModel(
    openContext: Navigator.CalendarChat.ChatOpenContext,
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
            visibleMediaLoading = false,
            listener = object : ChatListUiState.Listener {
                override fun onClickImage() {
                    viewModelScope.launch {
                        try {
                            viewModelStateFlow.update {
                                it.copy(isMediaLoading = true)
                            }
                            val media = platformRequest.getMedia()
                            viewModelStateFlow.update {
                                it.copy(
                                    selectedMedia = media,
                                )
                            }
                        } finally {
                            viewModelStateFlow.update {
                                it.copy(isMediaLoading = false)
                            }
                        }
                    }
                }

                override fun onClickVoice() {

                }

                override fun onClickSend(text: String) {
                    addRequest(
                        message = text,
                        uris = viewModelStateFlow.value.selectedMedia,
                    )
                    viewModelStateFlow.update {
                        it.copy(selectedMedia = listOf())
                    }
                }
            }
        )
    ).also { uiState ->
        viewModelScope.launch {
            when (openContext) {
                is Navigator.CalendarChat.ChatOpenContext.NewMessage -> {
                    val room = createRoom(builtinProjectId = openContext.builtinProjectId)
                    viewModelStateFlow.update {
                        it.copy(room = room)
                    }

                    val builtinProjectInfo = GetBuiltinProjectInfoUseCase().exec(
                        openContext.builtinProjectId,
                    )
                    viewModelStateFlow.update {
                        it.copy(builtinProjectInfo = builtinProjectInfo)
                    }
                    addRequest(
                        message = openContext.initialMessage,
                        uris = openContext.uriList,
                    )
                }

                is Navigator.CalendarChat.ChatOpenContext.OpenChat -> {
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
                        visibleMediaLoading = viewModelState.isMediaLoading,
                        items = viewModelState.chats.mapNotNull { chat ->
                            sequence {
                                yield(
                                    run message@{
                                        val message = chat.textMessage ?: return@message null
                                        when (chat.role) {
                                            Chat.Role.System -> {
                                                ChatListUiState.Message.Agent(
                                                    content = ChatListUiState.MessageContent.Text(message),
                                                )
                                            }

                                            Chat.Role.User,
                                            Chat.Role.Unknown -> {
                                                ChatListUiState.Message.User(
                                                    content = ChatListUiState.MessageContent.Text(message),
                                                )
                                            }

                                            Chat.Role.Assistant -> {
                                                ChatListUiState.Message.Agent(
                                                    content = ChatListUiState.MessageContent.Text(message),
                                                )
                                            }
                                        }
                                    }
                                )
                                yield(
                                    run image@{
                                        val uri = chat.imageUri ?: return@image null
                                        when (chat.role) {
                                            Chat.Role.System -> {
                                                ChatListUiState.Message.Agent(
                                                    content = ChatListUiState.MessageContent.Image(uri),
                                                )
                                            }

                                            Chat.Role.User,
                                            Chat.Role.Unknown -> {
                                                ChatListUiState.Message.User(
                                                    content = ChatListUiState.MessageContent.Image(uri),
                                                )
                                            }

                                            Chat.Role.Assistant -> {
                                                ChatListUiState.Message.Agent(
                                                    content = ChatListUiState.MessageContent.Image(uri),
                                                )
                                            }
                                        }
                                    }
                                )
                            }.filterNotNull()
                                .firstOrNull()
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

    private suspend fun createRoom(builtinProjectId: BuiltinProjectId?): ChatRoom {
        return withContext(Dispatchers.IO) {
            val room = ChatRoom(
                modelName = "gpt-4o-mini", // TODO SELECT
                builtInProjectId = builtinProjectId,
            )
            room.copy(
                id = ChatRoomId(appDatabase.chatRoomDao().insert(room))
            )
        }
    }

    private fun addRequest(message: String, uris: List<String> = listOf()) {
        if (message.isEmpty() && uris.isEmpty()) return

        viewModelScope.launch {
            val chatRoomId = viewModelStateFlow.value.room?.id ?: return@launch
            val chatDao = appDatabase.chatDao()
            val lastItem = chatDao.getChatRoomLastIndexItem(
                chatRoomId = chatRoomId.value,
            )
            val newChatIndex = lastItem?.index?.plus(1) ?: 0
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

            val chats = chatDao.get(chatRoomId = chatRoomId.value)
                .first()

            val systemMessage = run {
                val systemInfo = viewModelStateFlow.value.builtinProjectInfo ?: return@run null
                ChatGptClient.GptMessage(
                    role = ChatGptClient.GptMessage.Role.System,
                    contents = listOf(ChatGptClient.GptMessage.Content.Text(systemInfo.systemMessage))
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
            val response = getGptClient().request(
                messages = buildList {
                    add(systemMessage)
                    addAll(messages)
                }.filterNotNull(),
                format = viewModelStateFlow.value.builtinProjectInfo?.format
                    ?: ChatGptClient.Format.Text,
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
    }

    private suspend fun getGptClient(): ChatGptClient {
        return ChatGptClient(settingDataStore.getSecretKey())
    }

    private data class ViewModelState(
        val room: ChatRoom? = null,
        val chats: List<Chat> = listOf(),
        val selectedMedia: List<String> = listOf(),
        val isMediaLoading: Boolean = false,
        val builtinProjectInfo: GetBuiltinProjectInfoUseCase.Info? = null,
    )
}