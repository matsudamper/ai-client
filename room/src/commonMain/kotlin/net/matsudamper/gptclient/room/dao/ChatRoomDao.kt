package net.matsudamper.gptclient.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.matsudamper.gptclient.room.entity.ChatRoom
import net.matsudamper.gptclient.room.entity.ChatRoomWithStartChat

@Dao
interface ChatRoomDao {
    @Query("SELECT * FROM chat_room")
    fun getAll(): Flow<List<ChatRoom>>

    @Query(
        """
        SELECT * FROM chat_room
        JOIN chat ON chat_room.id = chat.chat_room_id
            AND chat.`index` = (
                SELECT MIN(`index`) FROM chat WHERE chat.chat_room_id = chat_room.id
            )
    """
    )
    fun getAllChatRoomWithStartChat(): Flow<List<ChatRoomWithStartChat>>

    @Query("SELECT * FROM chat_room where id = :chatRoomId")
    fun get(chatRoomId: Long): Flow<ChatRoom>

    @Insert
    suspend fun insert(chatRoom: ChatRoom): Long
}