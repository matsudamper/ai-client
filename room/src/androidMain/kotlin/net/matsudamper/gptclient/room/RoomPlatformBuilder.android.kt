package net.matsudamper.gptclient.room

import android.content.Context
import androidx.room.Room

object RoomPlatformBuilder {
    fun create(context: Context): AppDatabase = Room.databaseBuilder<AppDatabase>(
        context = context,
        "app-database",
    ).build()
}
