package net.matsudamper.gptclient.viewmodel

import net.matsudamper.gptclient.entity.Calendar
import net.matsudamper.gptclient.entity.Money
import net.matsudamper.gptclient.room.entity.BuiltinProjectId

class GetBuiltinProjectInfoUseCase {
    fun exec(builtinProjectId: BuiltinProjectId) : Info {
        return when(builtinProjectId) {
            BuiltinProjectId.Calendar -> {
                Info(
                    systemMessage = "それぞれ、Googleのカレンダーに予定を追加できるリンクを作成してください。複数あれば全て作成してください。日付の指定がなければ聞き返してください。",
                )
            }
            BuiltinProjectId.Money -> TODO()
            else -> throw NotImplementedError()
        }
    }

    data class Info(
        val systemMessage: String,
        val responseType: String = "",
    )
}