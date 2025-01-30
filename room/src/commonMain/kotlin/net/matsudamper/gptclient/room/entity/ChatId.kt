package net.matsudamper.gptclient.room.entity

import androidx.room.TypeConverter

@JvmInline
value class ChatId(val id: Int) {
    object Converter {
        @TypeConverter
        fun fromInt(value: Int): ChatId = ChatId(value)
        @TypeConverter
        fun toInt(value: ChatId): Int = value.id
    }
}