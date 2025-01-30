package net.matsudamper.gptclient.room

import androidx.room.Room

object RoomPlatformBuilder {
    fun create(): AppDatabase {
        return Room.databaseBuilder<AppDatabase>(
            "app-database"
        ).build()
    }
}
