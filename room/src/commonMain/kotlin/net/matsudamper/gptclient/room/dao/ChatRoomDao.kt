package net.matsudamper.gptclient.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import net.matsudamper.gptclient.room.entity.ChatRoom
import net.matsudamper.gptclient.room.entity.ChatRoomId
import net.matsudamper.gptclient.room.entity.ChatRoomWithSummary

@Dao
interface ChatRoomDao {
    @Query("SELECT * FROM chat_room")
    fun getAll(): Flow<List<ChatRoom>>

    @Query(
        """
        SELECT *, project.name as project_name FROM chat_room
        LEFT JOIN chat ON chat_room.id = chat.chat_room_id
            AND chat.`index` = (
                SELECT MIN(`index`) FROM chat WHERE chat.chat_room_id = chat_room.id AND chat.text_message != ''
            )
        LEFT JOIN project ON chat_room.project_id = project.id
        ORDER BY  
            CASE WHEN :isAsc == TRUE THEN chat.`create_date_at` END ASC,
            CASE WHEN :isAsc == FALSE THEN  chat.`create_date_at` END DESC
        """,
    )
    fun getAllChatRoomWithStartChat(isAsc: Boolean): Flow<List<ChatRoomWithSummary>>

    @Query("SELECT * FROM chat_room where id = :chatRoomId")
    fun get(chatRoomId: Long): Flow<ChatRoom>

    @Query(
        """
        SELECT * FROM chat_room
        LEFT JOIN chat ON chat_room.id = chat.chat_room_id
            AND chat.`index` = (
                SELECT MIN(`index`) FROM chat WHERE chat.chat_room_id = chat_room.id AND chat.text_message != ''
            )
        WHERE builtin_project_id = :builtInChatRoomId
        ORDER BY  
            CASE WHEN :isAsc == TRUE THEN chat_room.`create_date_at` END ASC,
            CASE WHEN :isAsc == FALSE THEN  chat_room.`create_date_at` END DESC
    """,
    )
    fun getFromBuiltInChatRoomId(builtInChatRoomId: String, isAsc: Boolean): Flow<List<ChatRoomWithSummary>>

    @Query(
        """
        SELECT * FROM chat_room
        LEFT JOIN chat ON chat_room.id = chat.chat_room_id
            AND chat.`index` = (
                SELECT MIN(`index`) FROM chat WHERE chat.chat_room_id = chat_room.id AND chat.text_message != ''
            )
        WHERE project_id = :projectId
        ORDER BY  
            CASE WHEN :isAsc == TRUE THEN chat_room.`create_date_at` END ASC,
            CASE WHEN :isAsc == FALSE THEN  chat_room.`create_date_at` END DESC
    """,
    )
    fun getFromProjectInChatRoomId(projectId: Long, isAsc: Boolean): Flow<List<ChatRoomWithSummary>>

    @Insert
    suspend fun insert(chatRoom: ChatRoom): Long

    @Update
    suspend fun update(chatRoom: ChatRoom)

    @Query("DELETE FROM chat_room where id = :chatRoomId")
    suspend fun delete(chatRoomId: Long)

    suspend fun update(id: ChatRoomId, block: (ChatRoom) -> Unit) : ChatRoom {
        val chatRoom = get(id.value).first()
        val new = chatRoom.apply(block)
        update(new)
        return new
    }
}
