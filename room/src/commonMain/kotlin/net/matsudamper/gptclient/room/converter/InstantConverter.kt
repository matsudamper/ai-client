package net.matsudamper.gptclient.room.converter

import androidx.room.TypeConverter
import java.time.Instant

class InstantConverter {
    @TypeConverter
    fun fromInstant(value: Long): Instant = Instant.ofEpochMilli(value)

    @TypeConverter
    fun toInstant(value: Instant): Long = value.toEpochMilli()
}
