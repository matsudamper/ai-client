package net.matsudamper.gptclient.room.entity

import androidx.room.TypeConverter

@JvmInline
value class BuiltinChatRoomId(val id: String) {
    object Converter {
        @TypeConverter
        fun fromString(value: String): BuiltinChatRoomId = BuiltinChatRoomId(value)

        @TypeConverter
        fun toString(value: BuiltinChatRoomId): String = value.id
    }
} 
