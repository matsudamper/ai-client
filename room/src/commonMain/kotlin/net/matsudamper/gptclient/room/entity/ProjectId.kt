package net.matsudamper.gptclient.room.entity

import androidx.room.TypeConverter

@JvmInline
value class ProjectId(val id: Int) {
    object Converter {
        @TypeConverter
        fun fromInt(value: Int): ProjectId = ProjectId(value)
        @TypeConverter
        fun toInt(value: ProjectId): Int = value.id
    }
}