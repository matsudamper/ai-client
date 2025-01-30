package net.matsudamper.gptclient.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.matsudamper.gptclient.room.entity.ChatRoom

@Dao
interface ChatRoomDao {
    @Query("SELECT * FROM chat_room")
    fun getAll(): Flow<List<ChatRoom>>

    @Query("SELECT * FROM chat_room where id = :chatRoomId")
    fun get(chatRoomId: Long): Flow<ChatRoom>

    @Insert
    suspend fun insert(chatRoom: ChatRoom) : Long
}