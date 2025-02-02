package net.matsudamper.gptclient.navigation

import androidx.compose.runtime.Immutable
import androidx.navigation.NavType
import kotlinx.serialization.Serializable
import net.matsudamper.gptclient.entity.ChatGptModel
import net.matsudamper.gptclient.room.entity.BuiltinProjectId
import net.matsudamper.gptclient.room.entity.ChatRoomId
import net.matsudamper.gptclient.room.entity.ProjectId
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
                val model: ChatGptModel,
            ) : ChatOpenContext

            @Serializable
            data class OpenChat(val chatRoomId: ChatRoomId) : ChatOpenContext
        }

        @Serializable
        sealed interface ChatType {
            @Serializable
            data object Normal: ChatType
            @Serializable
            data class BuiltinProject(val builtinProjectId: BuiltinProjectId): ChatType
            @Serializable
            data class Project(val projectId: ProjectId): ChatType
        }

        companion object {
            val typeMap: Map<KType, NavType<*>> = mapOf(
                typeOf<ChatOpenContext>() to JsonNavType<ChatOpenContext>(ChatOpenContext.serializer(), false),
            )
        }
    }

    @Serializable
    data class Project(
        val title: String,
        val type: ProjectType,
    ) {
        @Serializable
        sealed interface ProjectType {
            @Serializable
            data class Builtin(val builtinProjectId: BuiltinProjectId):ProjectType
            @Serializable
            data class Project(val projectId: ProjectId):ProjectType
        }
        companion object {
            val typeMap: Map<KType, NavType<*>> = mapOf(
                typeOf<ProjectType>() to JsonNavType<ProjectType>(ProjectType.serializer(), false),
            )
        }
    }

    @Serializable
    data object Settings : Navigator
}
