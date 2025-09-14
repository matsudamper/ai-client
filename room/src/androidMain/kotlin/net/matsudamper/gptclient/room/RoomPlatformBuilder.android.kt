package net.matsudamper.gptclient.room

import android.content.Context
import androidx.room.Room
import net.matsudamper.gptclient.room.migration.Migration1to2

object RoomPlatformBuilder {
    fun create(context: Context): AppDatabase = Room.databaseBuilder<AppDatabase>(
        context = context,
        "app-database",
    ).addMigrations(Migration1to2).build()
}
