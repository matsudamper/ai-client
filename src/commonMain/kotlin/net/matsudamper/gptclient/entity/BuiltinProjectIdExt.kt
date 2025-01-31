package net.matsudamper.gptclient.entity

import net.matsudamper.gptclient.room.entity.BuiltinProjectId

val BuiltinProjectId.Calendar
    get() = BuiltinProjectId("calendar")

val BuiltinProjectId.Money
    get() = BuiltinProjectId("money")
