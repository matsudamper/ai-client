package net.matsudamper.gptclient.room.entity

import androidx.room.Embedded
import androidx.room.Relation

data class ChatRoomWithStartChat(
    @Embedded val chatRoom: ChatRoom,
    @Relation(
        parentColumn = "id",
        entityColumn = "chat_room_id",
    ) val chat: Chat
)
