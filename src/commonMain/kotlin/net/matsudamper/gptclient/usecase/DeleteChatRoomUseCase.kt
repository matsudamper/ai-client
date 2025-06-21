package net.matsudamper.gptclient.usecase

import kotlinx.coroutines.flow.first
import net.matsudamper.gptclient.PlatformRequest
import net.matsudamper.gptclient.room.AppDatabase
import net.matsudamper.gptclient.room.entity.ChatRoomId

class DeleteChatRoomUseCase(private val appDatabase: AppDatabase, private val platformRequest: PlatformRequest) {
    suspend fun deleteChatRoom(chatRoomId: ChatRoomId) {
        val chatDao = appDatabase.chatDao()
        val allChat = chatDao.get(chatRoomId = chatRoomId.value)
        val allImageUri = allChat.first().mapNotNull { it.imageUri }

        val useImageUri = chatDao.getOtherChatroomUseImageUri(
            chatRoomId = chatRoomId.value,
            imageUriList = allImageUri,
        ).mapNotNull { it.imageUri }

        useImageUri.forEach { imageUri ->
            platformRequest.deleteFile(uri = imageUri)
        }

        appDatabase.chatDao().deleteByChatRoomId(
            chatRoomId = chatRoomId.value,
        )
        appDatabase.chatRoomDao().delete(chatRoomId = chatRoomId.value)
    }
}
