package net.matsudamper.gptclient.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import net.matsudamper.gptclient.room.converter.InstantConverter
import java.time.Instant

@Entity("chat_room")
@TypeConverters(
    InstantConverter::class,
    ChatRoomId.Converter::class,
    BuiltinChatRoomId.Converter::class,
    ProjectId.Converter::class,
)
data class ChatRoom(
    @ColumnInfo(name = "id") @PrimaryKey(autoGenerate = true) val id: ChatRoomId = ChatRoomId(0),
    @ColumnInfo(name = "project_id") val projectId: ProjectId? = null,
    @ColumnInfo(name = "builtin_project_id") val builtInProjectId: BuiltinChatRoomId? = null,
    @ColumnInfo(name = "model_name") val modelName: String,
    @ColumnInfo(name = "create_date_at") val createDateAt: Instant = Instant.now(),
)
