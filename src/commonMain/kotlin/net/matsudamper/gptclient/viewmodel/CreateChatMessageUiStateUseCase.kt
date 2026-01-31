package net.matsudamper.gptclient.viewmodel

import androidx.compose.ui.text.AnnotatedString
import net.matsudamper.gptclient.room.entity.Chat
import net.matsudamper.gptclient.ui.ChatListUiState
import net.matsudamper.gptclient.ui.chat.ChatMessageComposableInterface
import net.matsudamper.gptclient.ui.chat.ImageMessageComposableInterface
import net.matsudamper.gptclient.ui.chat.LoadingMessageComposableInterface
import net.matsudamper.gptclient.ui.chat.TextMessageComposableInterface

class CreateChatMessageUiStateUseCase {
    fun create(
        chats: List<Chat>,
        agentTransformer: (String) -> ChatMessageComposableInterface = { TextMessageComposableInterface(AnnotatedString(it)) },
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
                                    uiSet = TextMessageComposableInterface(AnnotatedString(message)),
                                )
                            }

                            Chat.Role.User,
                            Chat.Role.Unknown,
                            -> {
                                ChatListUiState.Message.User(
                                    uiSet = TextMessageComposableInterface(AnnotatedString(message)),
                                )
                            }

                            Chat.Role.Assistant -> {
                                ChatListUiState.Message.Agent(
                                    uiSet = agentTransformer(message),
                                )
                            }
                        }
                    },
                )
                yield(
                    run image@{
                        val uri = chat.imageUri ?: return@image null
                        when (chat.role) {
                            Chat.Role.System -> {
                                ChatListUiState.Message.Agent(
                                    uiSet = ImageMessageComposableInterface(ImageMessageComposableInterface.UiState(uri)),
                                )
                            }

                            Chat.Role.User,
                            Chat.Role.Unknown,
                            -> {
                                ChatListUiState.Message.User(
                                    uiSet = ImageMessageComposableInterface(ImageMessageComposableInterface.UiState(uri)),
                                )
                            }

                            Chat.Role.Assistant -> {
                                ChatListUiState.Message.Agent(
                                    uiSet = ImageMessageComposableInterface(ImageMessageComposableInterface.UiState(uri)),
                                )
                            }
                        }
                    },
                )
            }.filterNotNull()
                .firstOrNull()
        }.plus(
            ChatListUiState.Message.Agent(
                uiSet = LoadingMessageComposableInterface,
            ).takeIf { isChatLoading },
        ).filterNotNull()
    }
}
