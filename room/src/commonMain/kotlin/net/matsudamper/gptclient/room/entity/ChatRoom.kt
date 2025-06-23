package net.matsudamper.gptclient.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.time.Instant
import net.matsudamper.gptclient.room.converter.InstantConverter

@Entity("chat_room")
@TypeConverters(
    InstantConverter::class,
    ChatRoomId.Converter::class,
    BuiltinProjectId.Converter::class,
    ProjectId.Converter::class,
)
data class ChatRoom(
    @ColumnInfo(name = "id") @PrimaryKey(autoGenerate = true) val id: ChatRoomId = ChatRoomId(0),
    @ColumnInfo(name = "project_id") val projectId: ProjectId? = null,
    @ColumnInfo(name = "builtin_project_id") val builtInProjectId: BuiltinProjectId? = null,
    @ColumnInfo(name = "model_name") val modelName: String,
    @ColumnInfo(name = "summary") val summary: String?,
    @ColumnInfo(name = "create_date_at") val createDateAt: Instant = Instant.now(),
    @ColumnInfo(name = "worker_id") val workerId: String? = null,
)
