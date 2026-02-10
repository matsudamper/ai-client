package net.matsudamper.gptclient.room

import android.content.Context
import androidx.room.Room
import net.matsudamper.gptclient.room.migration.Migration1to2
import net.matsudamper.gptclient.room.migration.Migration2to3
import net.matsudamper.gptclient.room.migration.Migration3to4

object RoomPlatformBuilder {
    fun create(context: Context): AppDatabase = Room.databaseBuilder<AppDatabase>(
        context = context,
        "app-database",
    ).addMigrations(
        Migration1to2,
        Migration2to3,
        Migration3to4,
    ).build()
}
