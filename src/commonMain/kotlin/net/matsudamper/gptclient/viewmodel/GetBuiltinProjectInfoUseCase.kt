package net.matsudamper.gptclient.viewmodel

import androidx.compose.ui.text.AnnotatedString
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatterBuilder
import net.matsudamper.gptclient.entity.Calendar
import net.matsudamper.gptclient.entity.ChatGptModel
import net.matsudamper.gptclient.entity.Money
import net.matsudamper.gptclient.gpt.ChatGptClient
import net.matsudamper.gptclient.room.entity.BuiltinProjectId
import net.matsudamper.gptclient.usecase.CalendarResponseParser
import net.matsudamper.gptclient.usecase.MoneyResponseParser

class GetBuiltinProjectInfoUseCase {
    fun exec(builtinProjectId: BuiltinProjectId): Info {
        val date = DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd")
            .toFormatter()
            .format(LocalDate.now())
        return when (builtinProjectId) {
            BuiltinProjectId.Calendar -> {
                Info(
                    systemMessage = """
                        以下のJSONフォーマットに従ってください。
                        画像からカレンダーに予定を追加できる情報が欲しいです。複数あれば全て作成してください。日付の指定がなければ聞き返してください。
                        今日の日付は${date}です。
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
                    responseTransformer = {
                        CalendarResponseParser().toAnnotatedString(it)
                    },
                    summaryProvider = { CalendarResponseParser().parse(it)?.results?.firstOrNull()?.title },
                    model = ChatGptModel.Gpt4oMini,
                )
            }

            BuiltinProjectId.Money -> {
                Info(
                    systemMessage = """
                        error_messageが存在する場合はresultsは必ず空の配列でなければなりません。
                        画像から家計簿に追加できる情報が欲しいです。複数の商品があれば全て作成し、合計も最初に作成してください。画像に必要な情報が無ければerror_messageで聞き返してください。
                        合計のタイトル名は店名にしてください。1つしか商品が無い場合は合計が1つ、商品が1つのように作成してください。
                        今日の日付は${date}です。
                        時刻は全てOffsetなしとして扱ってください。
                        以下のJSONフォーマットに従ってください。
                        ```json
                        {
                            "error_message": "日付や金額が画像から見つからない場合に、ユーザーに入力を促すメッセージ(String?)",
                            "results": [
                                {
                                    "date": "日付、時間の ISO8601 Offsetなし (String)",
                                    "amount": "日本円の金額、ドル表記であればドルで良い(Int)",
                                    "title": "使用用途のタイトル(String)",
                                    "description": "補足情報(String?)"
                                }
                            ]
                        }
                        ```
                    """.trimIndent(),
                    format = ChatGptClient.Format.Json,
                    responseTransformer = {
                        MoneyResponseParser().toAnnotatedString(it)
                    },
                    model = ChatGptModel.Gpt4oMini,
                    summaryProvider = { MoneyResponseParser().parse(it)?.results?.firstOrNull()?.title },
                )
            }

            else -> throw NotImplementedError()
        }
    }

    data class Info(
        val systemMessage: String,
        val format: ChatGptClient.Format,
        val responseTransformer: (String) -> AnnotatedString,
        val summaryProvider: (String) -> String?,
        val model: ChatGptModel,
    )
}