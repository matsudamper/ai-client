package net.matsudamper.gptclient.navigation

import androidx.compose.runtime.Immutable
import androidx.navigation.NavType
import kotlinx.serialization.Serializable
import net.matsudamper.gptclient.room.entity.BuiltinProjectId
import net.matsudamper.gptclient.room.entity.ChatRoomId
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@Immutable
sealed interface Navigator {
    @Serializable
    data object StartChat : Navigator

    @Serializable
    data class Chat(
        val openContext: ChatOpenContext,
    ) : Navigator {
        @Serializable
        sealed interface ChatOpenContext {
            @Serializable
            data class NewMessage(
                val initialMessage: String,
                val uriList: List<String>,
                val chatType: ChatType,
            ) : ChatOpenContext

            @Serializable
            data class OpenChat(val chatRoomId: ChatRoomId) : ChatOpenContext
        }

        @Serializable
        sealed interface ChatType {
            data object Normal: ChatType
            data class Builtin(val builtinProjectId: BuiltinProjectId): ChatType
        }

        companion object {
            val typeMap: Map<KType, NavType<*>> = mapOf(
                typeOf<ChatOpenContext>() to JsonNavType<ChatOpenContext>(ChatOpenContext.serializer(), false),
            )
        }
    }

    @Serializable
    data class CalendarChat(
        val openContext: ChatOpenContext
    ) {
        @Serializable
        sealed interface ChatOpenContext {
            @Serializable
            data class NewMessage(
                val initialMessage: String,
                val uriList: List<String>,
            ) : ChatOpenContext

            @Serializable
            data class OpenChat(val chatRoomId: ChatRoomId) : ChatOpenContext
        }
        companion object {
            val typeMap: Map<KType, NavType<*>> = mapOf(
                typeOf<ChatOpenContext>() to JsonNavType<ChatOpenContext>(ChatOpenContext.serializer(), false),
            )
        }
    }

    @Serializable
    data class BuiltinProject(
        val title: String,
        val builtinProjectId: BuiltinProjectId,
    ) {
        companion object {
            val typeMap: Map<KType, NavType<*>> = mapOf(
                typeOf<BuiltinProjectId>() to JsonNavType<BuiltinProjectId>(BuiltinProjectId.serializer(), false),
            )
        }
    }

    @Serializable
    data object Settings : Navigator
}
