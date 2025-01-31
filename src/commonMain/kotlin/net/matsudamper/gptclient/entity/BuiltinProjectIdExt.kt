package net.matsudamper.gptclient.entity

import net.matsudamper.gptclient.room.entity.BuiltinProjectId

val BuiltinProjectId.Companion.Calendar
    get() = BuiltinProjectId("calendar")

val BuiltinProjectId.Companion.Money
    get() = BuiltinProjectId("money")
