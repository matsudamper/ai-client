package net.matsudamper.gptclient.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.matsudamper.gptclient.room.entity.Chat

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat where chat_room_id = :chatRoomId")
    fun get(chatRoomId: Long): Flow<List<Chat>>

    @Query("SELECT * FROM chat WHERE id IN (:userIds)")
    fun loadAllByIds(userIds: IntArray): Flow<List<Chat>>

    @Query(
        "SELECT * FROM chat WHERE id = :id"
    )
    fun findById(id: String): Flow<Chat>

    @Insert
    suspend fun insertAll(vararg chats: Chat)

    @Insert
    suspend fun insertAll(chats: List<Chat>)

    @Query("SELECT * FROM chat WHERE chat_room_id = :chatRoomId ORDER BY `index` DESC LIMIT 1")
    suspend fun getChatRoomLastIndexItem(chatRoomId: Long): Chat?

    @Query("DELETE FROM chat where id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM chat where chat_room_id = :chatRoomId")
    suspend fun deleteByChatRoomId(chatRoomId: Long)

    @Query("DELETE FROM chat where id = :id & `index` >= :index")
    suspend fun deleteItemsAtOrAfterIndex(id: String, index: Int)
}
