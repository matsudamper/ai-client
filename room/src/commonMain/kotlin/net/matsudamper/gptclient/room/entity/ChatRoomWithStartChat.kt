package net.matsudamper.gptclient.room.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class ChatRoomWithStartChat(
    @Embedded val chatRoom: ChatRoom,
    @ColumnInfo(name = "text_message") val textMessage: String?,
)
