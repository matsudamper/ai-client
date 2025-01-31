package net.matsudamper.gptclient.room.entity

import androidx.room.TypeConverter
import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class BuiltinProjectId(val id: String) {
    object Converter {
        @TypeConverter
        fun fromString(value: String): BuiltinProjectId = BuiltinProjectId(value)

        @TypeConverter
        fun toString(value: BuiltinProjectId): String = value.id
    }
    companion object;
} 
