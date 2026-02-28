package net.matsudamper.gptclient.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File
import net.matsudamper.gptclient.room.migration.Migration1to2

object RoomPlatformBuilder {
    fun create(databaseFile: File): AppDatabase {
        databaseFile.parentFile?.mkdirs()
        return Room
            .databaseBuilder<AppDatabase>(databaseFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .addMigrations(Migration1to2)
            .build()
    }
}
