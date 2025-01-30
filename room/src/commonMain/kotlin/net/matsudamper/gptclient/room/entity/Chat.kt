package net.matsudamper.gptclient.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import net.matsudamper.gptclient.room.converter.InstantConverter
import net.matsudamper.gptclient.room.entity.Chat.Role
import java.time.Instant

@Entity("chat")
@TypeConverters(
    InstantConverter::class,
    Role.Converter::class,
    ChatId.Converter::class,
    ChatRoomId.Converter::class
)
data class Chat(
    @ColumnInfo(name = "id") @PrimaryKey(autoGenerate = true) val id: ChatId = ChatId(0),
    @ColumnInfo(name = "chat_room_id") val chatRoomId: ChatRoomId,
    @ColumnInfo(name = "index") val index: Int,
    @ColumnInfo(name = "text_message") val textMessage: String?,
    @ColumnInfo(name = "image_message", typeAffinity = ColumnInfo.BLOB) val imageMessage: ByteArray?,
    @ColumnInfo(name = "role") val role: Role,
    @ColumnInfo(name = "create_date_at") val createDateAt: Instant = Instant.now(),
) {
    enum class Role(private val response: String) {
        System("system"),
        User("user"),
        Assistant("assistant"),
        Unknown("unknown")
        ;

        object Converter {
            @TypeConverter
            fun fromRole(role: Role): String = role.response

            @TypeConverter
            fun toRole(response: String): Role = entries.firstOrNull { it.response == response } ?: Unknown
        }
    }
}
