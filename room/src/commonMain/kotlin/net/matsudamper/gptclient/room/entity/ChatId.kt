package net.matsudamper.gptclient.room.entity

import androidx.room.TypeConverter

@JvmInline
value class ChatId(val id: Long) {
    object Converter {
        @TypeConverter
        fun from(value: Long): ChatId = ChatId(value)
        @TypeConverter
        fun to(value: ChatId): Long = value.id
    }
}