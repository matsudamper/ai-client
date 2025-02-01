package net.matsudamper.gptclient.viewmodel

import androidx.compose.ui.text.AnnotatedString
import net.matsudamper.gptclient.room.entity.Chat
import net.matsudamper.gptclient.ui.ChatListUiState

class CreateChatMessageUiStateUseCase() {
    fun create(
        chats: List<Chat>,
        agentTransformer: (String) -> AnnotatedString = { AnnotatedString(it) },
        isChatLoading: Boolean,
    ): List<ChatListUiState.Message> {
        return chats.mapNotNull { chat ->
            sequence {
                yield(
                    run message@{
                        val message = chat.textMessage ?: return@message null
                        when (chat.role) {
                            Chat.Role.System -> {
                                ChatListUiState.Message.Agent(
                                    content = ChatListUiState.MessageContent.Text(AnnotatedString(message)),
                                )
                            }

                            Chat.Role.User,
                            Chat.Role.Unknown -> {
                                ChatListUiState.Message.User(
                                    content = ChatListUiState.MessageContent.Text(AnnotatedString(message)),
                                )
                            }

                            Chat.Role.Assistant -> {
                                ChatListUiState.Message.Agent(
                                    content = ChatListUiState.MessageContent.Text(agentTransformer(message)),
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
        }.plus(
            ChatListUiState.Message.Agent(
                content = ChatListUiState.MessageContent.Loading,
            ).takeIf { isChatLoading }
        ).filterNotNull()
    }
}