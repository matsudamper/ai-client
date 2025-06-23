package net.matsudamper.gptclient.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import net.matsudamper.gptclient.room.migration.Migration1to2

object RoomPlatformBuilder {
    fun create(): AppDatabase {
        return Room
            .databaseBuilder<AppDatabase>(
                "app-database",
            )
            .setDriver(BundledSQLiteDriver())
            .addMigrations(Migration1to2)
            .build()
    }
}
