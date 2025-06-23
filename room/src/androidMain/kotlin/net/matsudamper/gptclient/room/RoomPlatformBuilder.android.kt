package net.matsudamper.gptclient.room

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import kotlin.use

object RoomPlatformBuilder {
    fun create(context: Context): AppDatabase = Room.databaseBuilder<AppDatabase>(
        context = context,
        "app-database",
    ).addMigrations(
        object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                connection.prepare("ALTER TABLE chat_room ADD COLUMN worker_id TEXT").use { statement ->
                    statement.step()
                }
            }
        },

    ).build()
}
