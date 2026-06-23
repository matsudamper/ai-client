package net.matsudamper.gptclient.room.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import kotlin.use

object Migration4to5 : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.prepare("ALTER TABLE project ADD COLUMN json_ui INTEGER NOT NULL DEFAULT 0").use { statement ->
            statement.step()
        }
    }
}
