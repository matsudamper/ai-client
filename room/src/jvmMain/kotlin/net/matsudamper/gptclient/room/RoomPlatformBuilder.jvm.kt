package net.matsudamper.gptclient.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import net.matsudamper.gptclient.room.migration.Migration1to2
import net.matsudamper.gptclient.room.migration.Migration2to3
import net.matsudamper.gptclient.room.migration.Migration3to4
import net.matsudamper.gptclient.room.migration.Migration4to5

object RoomPlatformBuilder {
    fun create(path: String): AppDatabase {
        return Room
            .databaseBuilder<AppDatabase>(
                path,
            )
            .setDriver(BundledSQLiteDriver())
            .addMigrations(
                Migration1to2,
                Migration2to3,
                Migration3to4,
                Migration4to5,
            )
            .build()
    }
}
