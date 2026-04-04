package net.matsudamper.gptclient.worker

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import net.matsudamper.gptclient.PlatformRequest
import net.matsudamper.gptclient.client.AiClient
import net.matsudamper.gptclient.client.gemini.GeminiClient
import net.matsudamper.gptclient.client.openai.ChatGptClient
import net.matsudamper.gptclient.datastore.SettingDataStore
import net.matsudamper.gptclient.entity.ChatGptModel
import net.matsudamper.gptclient.localmodel.LocalModelAiClientFactory
import net.matsudamper.gptclient.localmodel.LocalModelId
import net.matsudamper.gptclient.localmodel.LocalModelRepository
import net.matsudamper.gptclient.room.AppDatabase
import net.matsudamper.gptclient.room.entity.Chat
import net.matsudamper.gptclient.room.entity.ChatRoom
import net.matsudamper.gptclient.room.entity.ChatRoomId
import net.matsudamper.gptclient.util.Log
import net.matsudamper.gptclient.viewmodel.GetBuiltinProjectInfoUseCase

class ChatRequestRunner(
    private val appDatabase: AppDatabase,
    private val platformRequest: PlatformRequest,
    private val settingDataStore: SettingDataStore,
    private val localModelRepository: LocalModelRepository,
    private val localModelAiClientFactory: LocalModelAiClientFactory,
) {
    suspend fun run(
        chatRoomId: ChatRoomId,
    ): Result {
        return try {
            val room = appDatabase.chatRoomDao().get(chatRoomId = chatRoomId.value).first()
            val requestInfo = createRequestInfo(room)
            val chatModel = ChatGptModel.entries.firstOrNull { it.modelKey == requestInfo.modelKey }
                ?: run {
                    val localDef = localModelRepository.getModels()
                        .find { it.modelId.value == requestInfo.modelKey }
                    if (localDef != null) {
                        ChatGptModel.Local(
                            modelKey = localDef.modelId.value,
                            displayName = localDef.displayName,
                            enableImage = localDef.enableImage,
                            supportedImageMimeTypes = localDef.supportedImageMimeTypes,
                            defaultToken = localDef.defaultToken,
                        )
                    } else {
                        return fail(chatRoomId = chatRoomId, errorMessage = "モデルが見つかりません")
                    }
                }

            val gptClient = createClient(chatModel)
                ?: return fail(
                    chatRoomId = chatRoomId,
                    errorMessage = when {
                        chatModel is ChatGptModel.Local -> "モデルファイルが見つかりません。ダウンロードしてください"
                        chatModel is ChatGptModel.Remote.Gemini && chatModel.requireBillingKey -> "Gemini Billing Key が未設定です"
                        chatModel is ChatGptModel.Remote.Gemini -> "Gemini API Key が未設定です"
                        else -> "APIキーが未設定です"
                    },
                )

            val response = when (
                val response = gptClient.request(
                    messages = createMessage(
                        systemMessage = requestInfo.systemMessage,
                        chatRoomId = chatRoomId,
                    ),
                    format = requestInfo.format,
                )
            ) {
                is AiClient.GptResult.Error -> {
                    return fail(chatRoomId = chatRoomId, errorMessage = response.reason.message)
                }

                is AiClient.GptResult.Success -> response.response
            }

            writeResponse(chatRoomId = chatRoomId, response = response)
            clearWorkerState(chatRoomId = chatRoomId)

            Result.Success
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
            fail(
                chatRoomId = chatRoomId,
                errorMessage = throwable.message?.takeIf { it.isNotBlank() } ?: "エラーが発生しました",
            )
        }
    }

    private suspend fun createRequestInfo(room: ChatRoom): RequestInfo {
        return when (val builtinProjectId = room.builtInProjectId) {
            null -> when (val projectId = room.projectId) {
                null -> {
                    RequestInfo(
                        format = AiClient.Format.Text,
                        systemMessage = null,
                        modelKey = room.modelKey,
                    )
                }

                else -> {
                    val project = appDatabase.projectDao().get(projectId.id).first()
                    RequestInfo(
                        format = AiClient.Format.Text,
                        systemMessage = project?.systemMessage,
                        modelKey = room.modelKey,
                    )
                }
            }

            else -> {
                val builtinProjectInfo = GetBuiltinProjectInfoUseCase().exec(
                    builtinProjectId = builtinProjectId,
                    platformRequest = platformRequest,
                )
                RequestInfo(
                    format = builtinProjectInfo.format,
                    systemMessage = builtinProjectInfo.systemMessage,
                    modelKey = room.modelKey,
                )
            }
        }
    }

    private suspend fun createClient(chatModel: ChatGptModel): AiClient? {
        return when (chatModel) {
            is ChatGptModel.Remote.Gpt -> ChatGptClient(
                secretKey = settingDataStore.getSecretKey(),
                model = chatModel,
            )

            is ChatGptModel.Remote.Gemini -> {
                val apiKey = if (chatModel.requireBillingKey) {
                    settingDataStore.getGeminiBillingKey()
                } else {
                    settingDataStore.getGeminiSecretKey()
                }
                apiKey.takeIf { it.isNotBlank() }?.let { GeminiClient(apiKey = it, model = chatModel) }
            }

            is ChatGptModel.Local -> createLocalAiClient(chatModel)
            else -> null
        }
    }

    private fun createLocalAiClient(model: ChatGptModel.Local): AiClient? {
        return localModelAiClientFactory.create(LocalModelId(model.modelKey))
    }

    private suspend fun writeResponse(
        chatRoomId: ChatRoomId,
        response: AiClient.AiResponse,
    ) {
        val chatDao = appDatabase.chatDao()
        val lastItem = chatDao.getChatRoomLastIndexItem(
            chatRoomId = chatRoomId.value,
        )
        val newChatIndex = lastItem?.index?.plus(1) ?: 0

        val roomChats = response.choices.mapIndexed { index, choice ->
            Chat(
                chatRoomId = chatRoomId,
                index = newChatIndex + index,
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
            .first()
            .lastOrNull { it.role == Chat.Role.User }
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
                builtinProjectInfo.summaryProvider.provide(firstInstruction, lastInstruction, message.content)
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

        val systemRequest = systemMessage?.let {
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
                val textMessage = chat.textMessage
                if (textMessage != null) {
                    add(AiClient.GptMessage.Content.Text(textMessage))
                }
                val imageMessage = chat.imageUri
                if (imageMessage != null) {
                    val imageData = platformRequest.readImageData(uri = imageMessage)
                    if (imageData == null) {
                        return listOf()
                    }
                    add(
                        AiClient.GptMessage.Content.Base64Image(
                            @OptIn(ExperimentalEncodingApi::class)
                            Base64.encode(imageData.bytes),
                            mimeType = imageData.mimeType,
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
            add(systemRequest)
            addAll(messages)
        }.filterNotNull()
    }

    private suspend fun clearWorkerState(chatRoomId: ChatRoomId) {
        appDatabase.chatRoomDao().update(id = chatRoomId) {
            it.copy(
                workerId = null,
                latestErrorMessage = null,
            )
        }
    }

    private suspend fun fail(
        chatRoomId: ChatRoomId,
        errorMessage: String,
    ): Result.Error {
        Log.e("ChatRequestRunner", errorMessage)
        appDatabase.chatRoomDao().update(id = chatRoomId) {
            it.copy(
                workerId = null,
                latestErrorMessage = errorMessage,
            )
        }
        return Result.Error(errorMessage = errorMessage)
    }

    private data class RequestInfo(
        val format: AiClient.Format,
        val systemMessage: String?,
        val modelKey: String,
    )

    sealed interface Result {
        data object Success : Result
        data class Error(val errorMessage: String) : Result
    }
}
