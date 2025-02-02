package net.matsudamper.gptclient.room.entity

import androidx.room.TypeConverter
import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class ProjectId(val id: Long) {
    object Converter {
        @TypeConverter
        fun from(value: Long): ProjectId = ProjectId(value)
        @TypeConverter
        fun to(value: ProjectId): Long = value.id
    }
}
