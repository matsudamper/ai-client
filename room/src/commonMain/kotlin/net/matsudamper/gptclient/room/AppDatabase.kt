package net.matsudamper.gptclient.room

import androidx.room.Database
import androidx.room.RoomDatabase
import net.matsudamper.gptclient.room.dao.ChatDao
import net.matsudamper.gptclient.room.dao.ChatRoomDao
import net.matsudamper.gptclient.room.dao.ProjectDao
import net.matsudamper.gptclient.room.entity.Chat
import net.matsudamper.gptclient.room.entity.ChatRoom
import net.matsudamper.gptclient.room.entity.Project

@Database(
    entities = [
        Chat::class,
        ChatRoom::class,
        Project::class,
    ],
    version = 1,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun chatRoomDao(): ChatRoomDao
    abstract fun projectDao(): ProjectDao
}
