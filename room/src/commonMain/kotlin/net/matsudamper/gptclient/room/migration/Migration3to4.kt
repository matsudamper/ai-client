package net.matsudamper.gptclient.room.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import kotlin.use

object Migration3to4 : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.prepare("ALTER TABLE chat_room ADD COLUMN use_gemini_billing_key INTEGER").use { statement ->
            statement.step()
        }
    }
}
