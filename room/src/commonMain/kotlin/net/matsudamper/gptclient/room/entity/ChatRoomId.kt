package net.matsudamper.gptclient.room.entity

import androidx.room.TypeConverter

@JvmInline
value class ChatRoomId(val value: Int) {
    object Converter {
        @TypeConverter
        fun fromInt(value: Int): ChatRoomId = ChatRoomId(value)
        @TypeConverter
        fun toInt(value: ChatRoomId): Int = value.value
    }
}
