package net.matsudamper.gptclient.viewmodel

import net.matsudamper.gptclient.entity.Calendar
import net.matsudamper.gptclient.entity.Money
import net.matsudamper.gptclient.gpt.ChatGptClient
import net.matsudamper.gptclient.room.entity.BuiltinProjectId

class GetBuiltinProjectInfoUseCase {
    fun exec(builtinProjectId: BuiltinProjectId) : Info {
        return when(builtinProjectId) {
            BuiltinProjectId.Calendar -> {
                Info(
                    systemMessage = """
                        以下のJSONフォーマットに従ってください。
                        画像からカレンダーに予定を追加できる情報が欲しいです。複数あれば全て作成してください。日付の指定がなければ聞き返してください。
                        あなたは今日の日付を知りません。
                        error_messageが存在する場合はresultsは必ず空の配列でなければなりません。
                        時刻は全てOffsetなしとして扱ってください。
                        ```json
                        {
                            "error_message": "日付や日時が指定されていない場合に、ユーザーに入力を促すメッセージ(String?)",
                            "results": [
                                {
                                    "start_date": "開始日付、時間のISO8601(String)",
                                    "end_date": "終了日付、時間のISO8601。無ければ、内容から推測して12時間以内で設定して(String)",
                                    "title": "カレンダーのタイトル(String)",
                                    "location": "nullable, 場所の名前(String?)",
                                    "description": "補足情報(String?)"
                                }
                            ]
                        }
                        ```
                    """.trimIndent(),
                    format = ChatGptClient.Format.Json,
                )
            }
            BuiltinProjectId.Money -> TODO()
            else -> throw NotImplementedError()
        }
    }

    data class Info(
        val systemMessage: String,
        val responseType: String = "",
        val format: ChatGptClient.Format,
    )
}