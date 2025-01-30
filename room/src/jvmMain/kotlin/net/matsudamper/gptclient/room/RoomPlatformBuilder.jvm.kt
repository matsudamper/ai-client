package net.matsudamper.gptclient.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

object RoomPlatformBuilder {
    fun create(): AppDatabase {
        return Room
            .databaseBuilder<AppDatabase>(
                "app-database"
            )
            .setDriver(BundledSQLiteDriver())
            .build()
    }
}
