package net.matsudamper.gptclient.usecase

import kotlinx.coroutines.flow.first
import net.matsudamper.gptclient.room.AppDatabase
import net.matsudamper.gptclient.room.entity.ChatRoomId

class DeleteChatRoomUseCase(private val appDatabase: AppDatabase) {
    suspend fun deleteChatRoom(
        chatRoomId: ChatRoomId,
        deleteFile: suspend (String) -> Boolean,
    ) {
        val chatDao = appDatabase.chatDao()
        val allChat = chatDao.get(chatRoomId = chatRoomId.value)
        val allImageUri = allChat.first().mapNotNull { it.imageUri }

        val useImageUri = chatDao.getOtherChatroomUseImageUri(
            chatRoomId = chatRoomId.value,
            imageUriList = allImageUri,
        ).mapNotNull { it.imageUri }

        useImageUri.forEach { imageUri ->
            deleteFile(imageUri)
        }

        appDatabase.chatDao().deleteByChatRoomId(
            chatRoomId = chatRoomId.value,
        )
        appDatabase.chatRoomDao().delete(chatRoomId = chatRoomId.value)
    }
}
