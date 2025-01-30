package net.matsudamper.gptclient.room.entity

import androidx.room.TypeConverter

@JvmInline
value class ChatRoomId(val value: Long) {
    object Converter {
        @TypeConverter
        fun fromInt(value: Long): ChatRoomId = ChatRoomId(value)
        @TypeConverter
        fun toInt(value: ChatRoomId): Long = value.value
    }
}
