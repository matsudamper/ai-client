package net.matsudamper.gptclient.room.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class ChatRoomWithSummary(
    @Embedded val chatRoom: ChatRoom,
    @ColumnInfo(name = "text_message") val textMessage: String?,
    @ColumnInfo(name = "project_name") val projectName: String?,
)
