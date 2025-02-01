package net.matsudamper.gptclient.viewmodel.calendar

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.http.encodeURLParameter
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
import kotlinx.serialization.json.Json
import net.matsudamper.gptclient.PlatformRequest
import net.matsudamper.gptclient.entity.Calendar
import net.matsudamper.gptclient.navigation.Navigator
import net.matsudamper.gptclient.room.AppDatabase
import net.matsudamper.gptclient.room.entity.BuiltinProjectId
import net.matsudamper.gptclient.room.entity.Chat
import net.matsudamper.gptclient.room.entity.ChatRoom
import net.matsudamper.gptclient.room.entity.ChatRoomId
import net.matsudamper.gptclient.ui.ChatListUiState
import net.matsudamper.gptclient.viewmodel.AddRequestUseCase
import net.matsudamper.gptclient.viewmodel.CreateChatMessageUiStateUseCase
import net.matsudamper.gptclient.viewmodel.GetBuiltinProjectInfoUseCase
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoField


class CalendarChatViewModel(
    openContext: Navigator.CalendarChat.ChatOpenContext,
    private val platformRequest: PlatformRequest,
    private val appDatabase: AppDatabase,
    private val insertDataAndAddRequestUseCase: AddRequestUseCase,
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
            val builtinProjectInfo = GetBuiltinProjectInfoUseCase().exec(
                BuiltinProjectId.Calendar,
            )
            viewModelStateFlow.update {
                it.copy(builtinProjectInfo = builtinProjectInfo)
            }
            when (openContext) {
                is Navigator.CalendarChat.ChatOpenContext.NewMessage -> {
                    val room = createRoom(builtinProjectId = BuiltinProjectId.Calendar)
                    viewModelStateFlow.update {
                        it.copy(room = room)
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
                        items = CreateChatMessageUiStateUseCase().create(
                            chats = viewModelState.chats,
                            agentTransformer = { original ->
                                calendarResponseTransformer(original)
                            },
                            isChatLoading = viewModelState.isChatLoading,
                        ),
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

    private fun calendarResponseTransformer(original: String): AnnotatedString {
        return try {
            val result = Json.decodeFromString<CalendarGptResponse>(original)
            if (result.results.isEmpty()) {
                AnnotatedString(result.errorMessage ?: original)
            } else {
                buildAnnotatedString {
                    for (result in result.results) {
                        appendLine(result.title)
                        appendLine("日時: ${result.startDate.toDisplayFormat()}~${result.endDate.toDisplayFormat()}")
                        appendLine("場所: ${result.location}")
                        appendLine("説明: ${result.description}")

                        val googleCalendarUrl = "https://calendar.google.com/calendar/render" +
                                "?action=TEMPLATE" +
                                "&text=${result.title.encodeURLParameter()}" +
                                "&dates=${result.startDate.toGoogleCalendarFormat()}/${result.endDate.toGoogleCalendarFormat()}" +
                                "&details=${
                                    result.description.orEmpty().encodeURLParameter()
                                }" +
                                "&location=${result.location.orEmpty().encodeURLParameter()}"
                        pushLink(LinkAnnotation.Url(googleCalendarUrl))
                        withStyle(SpanStyle(color = Color.Blue)) {
                            appendLine("Google Calendar追加リンク")
                        }
                        pop()
                        appendLine()
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            AnnotatedString(original)
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
        val systemInfo = viewModelStateFlow.value.builtinProjectInfo ?: return
        val chatRoomId = viewModelStateFlow.value.room?.id ?: return

        viewModelScope.launch {
            try {
                viewModelStateFlow.update {
                    it.copy(isChatLoading = true)
                }
                insertDataAndAddRequestUseCase.add(
                    chatRoomId = chatRoomId,
                    message = message,
                    uris = uris,
                    systemMessage = systemInfo.systemMessage,
                    format = systemInfo.format,
                )
            }finally {
                viewModelStateFlow.update {
                    it.copy(isChatLoading = false)
                }
            }
        }
    }

    private fun LocalDateTime.toGoogleCalendarFormat(): String {
        with(
            atZone(ZoneId.systemDefault()).toOffsetDateTime()
                .withOffsetSameInstant(ZoneOffset.UTC)
        ) {
            val year = get(ChronoField.YEAR)
            val month = get(ChronoField.MONTH_OF_YEAR).toString().padStart(2, '0')
            val dayOfMonth = get(ChronoField.DAY_OF_MONTH).toString().padStart(2, '0')
            val hour = get(ChronoField.HOUR_OF_DAY).toString().padStart(2, '0')
            val minute = get(ChronoField.MINUTE_OF_HOUR).toString().padStart(2, '0')
            return "$year$month${dayOfMonth}T${hour}${minute}00Z"
        }
    }

    private fun LocalDateTime.toDisplayFormat(): String {
        with(atZone(ZoneId.systemDefault()).toOffsetDateTime()) {
            val year = get(ChronoField.YEAR)
            val month = get(ChronoField.MONTH_OF_YEAR).toString().padStart(2, '0')
            val dayOfMonth = get(ChronoField.DAY_OF_MONTH).toString().padStart(2, '0')
            val hour = get(ChronoField.HOUR_OF_DAY).toString().padStart(2, '0')
            val minute = get(ChronoField.MINUTE_OF_HOUR).toString().padStart(2, '0')
            return "$year/$month/${dayOfMonth} ${hour}:${minute}"
        }
    }

    private data class ViewModelState(
        val room: ChatRoom? = null,
        val chats: List<Chat> = listOf(),
        val selectedMedia: List<String> = listOf(),
        val isMediaLoading: Boolean = false,
        val builtinProjectInfo: GetBuiltinProjectInfoUseCase.Info? = null,
        val isChatLoading: Boolean = false,
    )
}