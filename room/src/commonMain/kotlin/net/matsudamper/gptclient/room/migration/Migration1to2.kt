package net.matsudamper.gptclient.room.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import kotlin.use

object Migration1to2 : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.prepare("ALTER TABLE chat_room ADD COLUMN worker_id TEXT").use { statement ->
            statement.step()
        }
    }
}