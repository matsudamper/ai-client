package net.matsudamper.gptclient.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import net.matsudamper.gptclient.room.migration.Migration1to2
import net.matsudamper.gptclient.room.migration.Migration2to3
import net.matsudamper.gptclient.room.migration.Migration3to4

object RoomPlatformBuilder {
    fun create(): AppDatabase {
        return Room
            .databaseBuilder<AppDatabase>(
                "app-database",
            )
            .setDriver(BundledSQLiteDriver())
            .addMigrations(
                Migration1to2,
                Migration2to3,
                Migration3to4,
            )
            .build()
    }
}
