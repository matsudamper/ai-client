package net.matsudamper.gptclient.room.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import kotlin.use

object Migration2to3 : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.prepare("ALTER TABLE chat_room ADD COLUMN latest_error_message TEXT").use { statement ->
            statement.step()
        }
    }
}
