package net.matsudamper.gptclient.entity

import net.matsudamper.gptclient.room.entity.BuiltinProjectId

val BuiltinProjectId.Companion.Calendar
    get() = BuiltinProjectId("calendar")

val BuiltinProjectId.Companion.Money
    get() = BuiltinProjectId("money")

val BuiltinProjectId.Companion.Emoji
    get() = BuiltinProjectId("emoji")

fun BuiltinProjectId.getName(): String = when (this) {
    BuiltinProjectId.Calendar -> "カレンダー"
    BuiltinProjectId.Money -> "家計簿"
    BuiltinProjectId.Companion.Emoji -> "絵文字"
    else -> throw NotImplementedError("Not yet implemented ${this.id}")
}
