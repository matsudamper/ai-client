package net.matsudamper.gptclient.entity

import net.matsudamper.gptclient.room.entity.BuiltinProjectId

val BuiltinProjectId.Companion.Calendar
    get() = BuiltinProjectId("calendar")

val BuiltinProjectId.Companion.Money
    get() = BuiltinProjectId("money")


fun BuiltinProjectId.getName(): String {
    return when (this) {
        BuiltinProjectId.Calendar -> "カレンダー"
        BuiltinProjectId.Money -> "家計簿"
        else -> throw NotImplementedError("Not yet implemented ${this.id}")
    }
}
