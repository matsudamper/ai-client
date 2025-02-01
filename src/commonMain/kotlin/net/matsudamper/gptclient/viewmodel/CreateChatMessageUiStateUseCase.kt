package net.matsudamper.gptclient.viewmodel

import net.matsudamper.gptclient.room.entity.Chat
import net.matsudamper.gptclient.ui.ChatListUiState

class CreateChatMessageUiStateUseCase() {
    fun create(chats: List<Chat>): List<ChatListUiState.Message> {
        return chats.mapNotNull { chat ->
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
        }
    }
}