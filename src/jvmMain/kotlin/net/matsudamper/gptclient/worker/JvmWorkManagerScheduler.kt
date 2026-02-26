package net.matsudamper.gptclient.worker

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.launch
import net.matsudamper.gptclient.PlatformRequest
import net.matsudamper.gptclient.client.AiClient
import net.matsudamper.gptclient.client.gemini.GeminiClient
import net.matsudamper.gptclient.client.openai.ChatGptClient
import net.matsudamper.gptclient.datastore.SettingDataStore
import net.matsudamper.gptclient.entity.ApiProvider
import net.matsudamper.gptclient.entity.ChatGptModel
import net.matsudamper.gptclient.room.AppDatabase
import net.matsudamper.gptclient.room.entity.Chat
import net.matsudamper.gptclient.room.entity.ChatRoomId
import net.matsudamper.gptclient.viewmodel.AddRequestUseCase
import net.matsudamper.gptclient.viewmodel.GetBuiltinProjectInfoUseCase

class JvmWorkManagerScheduler(
    private val appDatabase: AppDatabase,
    private val platformRequest: PlatformRequest,
    private val settingDataStore: SettingDataStore,
) : AddRequestUseCase.WorkManagerScheduler {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val works = ConcurrentHashMap<String, Job>()
    private val roomToWork = ConcurrentHashMap<Long, String>()

    override fun scheduleWork(
        chatRoomId: ChatRoomId,
        message: String,
        uris: List<String>,
    ): String {
        val workId = UUID.randomUUID().toString()
        roomToWork.remove(chatRoomId.value)?.let { activeWorkId ->
            works.remove(activeWorkId)?.cancel()
        }

        val job = scope.launch {
            runCatching {
                runRequest(chatRoomId)
            }.onFailure { throwable ->
                appDatabase.chatRoomDao().update(id = chatRoomId) { room ->
                    room.copy(
                        workerId = null,
                        latestErrorMessage = room.latestErrorMessage ?: "エラーが発生しました",
                    )
                }
                platformRequest.showToast("処理に失敗しました: ${throwable.message ?: "Unknown error"}")
            }
        }
        works[workId] = job
        roomToWork[chatRoomId.value] = workId
        job.invokeOnCompletion {
            works.remove(workId)
            roomToWork.remove(chatRoomId.value, workId)
        }
        return workId
    }

    override fun isWorkRunning(workId: String): Boolean {
        return works[workId]?.isActive == true
    }

    private suspend fun runRequest(chatRoomId: ChatRoomId) {
        val chatRoomDao = appDatabase.chatRoomDao()
        val room = chatRoomDao.get(chatRoomId = chatRoomId.value).first()
        val roomInfo = resolveRoomInfo(chatRoomId = chatRoomId)
        val chatModel = ChatGptModel.entries.firstOrNull { it.modelName == roomInfo.modelName }
            ?: run {
                chatRoomDao.update(room.copy(workerId = null, latestErrorMessage = "モデルが見つかりません"))
                platformRequest.showToast("モデルが見つかりません")
                return
            }

        val client: AiClient = when (chatModel.provider) {
            ApiProvider.OpenAI -> ChatGptClient(secretKey = settingDataStore.getSecretKey())
            ApiProvider.Gemini -> GeminiClient(apiKey = settingDataStore.getGeminiSecretKey())
        }

        val response = client.request(
            messages = createMessage(
                systemMessage = roomInfo.systemMessage,
                chatRoomId = chatRoomId,
            ),
            format = roomInfo.format,
            model = chatModel,
        )

        when (response) {
            is AiClient.GptResult.Error -> {
                chatRoomDao.update(room.copy(workerId = null, latestErrorMessage = response.reason.message))
                platformRequest.showToast("処理失敗: ${response.reason.message}")
                return
            }

            is AiClient.GptResult.Success -> {
                val message = response.response.choices
                    .lastOrNull { it.message.role == AiClient.AiResponse.Choice.Role.Assistant }
                    ?.message?.content
                    ?: ""

                val newIndex = (appDatabase.chatDao().getChatRoomLastIndexItem(chatRoomId.value)?.index ?: -1) + 1
                appDatabase.chatDao().insertAll(
                    Chat(
                        chatRoomId = chatRoomId,
                        index = newIndex,
                        textMessage = message,
                        imageUri = null,
                        role = Chat.Role.Assistant,
                    ),
                )
                updateSummary(chatRoomId = chatRoomId, response = response.response)
                chatRoomDao.update(room.copy(workerId = null, latestErrorMessage = null))
                platformRequest.showToast("応答を受信しました")
            }
        }
    }

    private suspend fun resolveRoomInfo(chatRoomId: ChatRoomId): RoomInfo {
        val room = appDatabase.chatRoomDao().get(chatRoomId = chatRoomId.value).first()
        return when (val builtinProjectId = room.builtInProjectId) {
            null -> when (val projectId = room.projectId) {
                null -> RoomInfo(AiClient.Format.Text, null, room.modelName)
                else -> {
                    val project = appDatabase.projectDao().get(projectId.id).first()
                    RoomInfo(AiClient.Format.Text, project?.systemMessage, room.modelName)
                }
            }

            else -> {
                val info = GetBuiltinProjectInfoUseCase().exec(
                    builtinProjectId = builtinProjectId,
                    platformRequest = platformRequest,
                )
                RoomInfo(info.format, info.systemMessage, room.modelName)
            }
        }
    }

    private suspend fun updateSummary(
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
        val assistantText = response.choices
            .lastOrNull { it.message.role == AiClient.AiResponse.Choice.Role.Assistant }
            ?.message
            ?.content
            ?: return

        val summary = when (val builtinProjectId = room.builtInProjectId) {
            null -> assistantText.take(50).takeIf { it.isNotBlank() }?.let {
                if (assistantText.length > 50) "$it..." else it
            }

            else -> {
                val builtinProjectInfo = GetBuiltinProjectInfoUseCase().exec(
                    builtinProjectId = builtinProjectId,
                    platformRequest = platformRequest,
                )
                builtinProjectInfo.summaryProvider.provide(firstInstruction, lastInstruction, assistantText)
            }
        }

        if (summary != null) {
            chatRoomDao.update(room.copy(summary = summary))
        }
    }

    private suspend fun createMessage(
        systemMessage: String?,
        chatRoomId: ChatRoomId,
    ): List<AiClient.GptMessage> {
        val chats = appDatabase.chatDao().get(chatRoomId = chatRoomId.value).first()

        val system = systemMessage?.let {
            AiClient.GptMessage(
                role = AiClient.GptMessage.Role.System,
                contents = listOf(AiClient.GptMessage.Content.Text(it)),
            )
        }

        val messages = chats.map { chat ->
            val role = when (chat.role) {
                Chat.Role.System -> AiClient.GptMessage.Role.System
                Chat.Role.User -> AiClient.GptMessage.Role.User
                Chat.Role.Assistant -> AiClient.GptMessage.Role.Assistant
                Chat.Role.Unknown -> AiClient.GptMessage.Role.User
            }

            val contents = buildList {
                chat.textMessage?.let { add(AiClient.GptMessage.Content.Text(it)) }
                chat.imageUri?.let { uri ->
                    val byteArray = platformRequest.readPngByteArray(uri) ?: return listOf()
                    add(
                        AiClient.GptMessage.Content.Base64Image(
                            @OptIn(ExperimentalEncodingApi::class)
                            Base64.encode(byteArray),
                        ),
                    )
                }
            }
            AiClient.GptMessage(role = role, contents = contents)
        }

        return buildList {
            add(system)
            addAll(messages)
        }.filterNotNull()
    }

    private data class RoomInfo(
        val format: AiClient.Format,
        val systemMessage: String?,
        val modelName: String,
    )
}
