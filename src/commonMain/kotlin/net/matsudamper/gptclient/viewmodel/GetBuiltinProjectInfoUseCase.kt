package net.matsudamper.gptclient.viewmodel

import java.time.LocalDate
import java.time.format.DateTimeFormatterBuilder
import net.matsudamper.gptclient.PlatformRequest
import net.matsudamper.gptclient.entity.Calendar
import net.matsudamper.gptclient.entity.ChatGptModel
import net.matsudamper.gptclient.entity.Emoji
import net.matsudamper.gptclient.entity.Money
import net.matsudamper.gptclient.gpt.ChatGptClientInterface
import net.matsudamper.gptclient.room.entity.BuiltinProjectId
import net.matsudamper.gptclient.ui.chat.ChatMessageComposableInterface
import net.matsudamper.gptclient.ui.chat.TextMessageComposableInterface
import net.matsudamper.gptclient.usecase.CalendarResponseParser
import net.matsudamper.gptclient.usecase.EmojiResponseParser
import net.matsudamper.gptclient.usecase.MoneyResponseParser

class GetBuiltinProjectInfoUseCase {
    fun exec(
        builtinProjectId: BuiltinProjectId,
        platformRequest: PlatformRequest,
    ): Info {
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
                        日時はISO8601形式で、日付と時刻の間は必ず「T」で区切ってください(例: "2025-03-20T15:30:00")。スペース区切りは不可です。
                        ```json
                        {
                            "error_message": "日付や日時が指定されていない場合に、ユーザーに入力を促すメッセージ(String?)",
                            "results": [
                                {
                                    "start_date": "開始日付、時間のISO8601(例: 2025-01-20T09:00:00)(String)",
                                    "end_date": "終了日付、時間のISO8601(例: 2025-01-20T17:00:00)。無ければ、内容から推測して12時間以内で設定して(String)",
                                    "title": "カレンダーのタイトル(String)",
                                    "location": "nullable, 場所の名前(String?)",
                                    "description": "補足情報(String?)"
                                }
                            ]
                        }
                        ```
                    """.trimIndent(),
                    format = ChatGptClientInterface.Format.Json,
                    responseTransformer = {
                        TextMessageComposableInterface(CalendarResponseParser().toAnnotatedString(it))
                    },
                    summaryProvider = {
                        val parsed = CalendarResponseParser().parse(it)
                        parsed?.results?.lastOrNull()?.title ?: parsed?.errorMessage
                    },
                    model = ChatGptModel.GeminiFlashLatest,
                )
            }

            BuiltinProjectId.Money -> {
                Info(
                    systemMessage = """
                        error_messageが存在する場合はresultsは必ず空の配列でなければなりません。
                        画像から家計簿に追加できる情報が欲しいです。複数の商品があれば全て作成し、合計も最初に作成してください(タイトルは店名で、無ければサマリを設定して)。画像に必要な情報が無ければerror_messageで聞き返してください。
                        今日の日付は${date}です。
                        時刻は全てOffsetなしとして扱ってください。
                        日時はISO8601形式で、日付と時刻の間は必ず「T」で区切ってください(例: "2025-03-20T15:30:00")。スペース区切りは不可です。
                        以下のJSONフォーマットに従ってください。
                        ```json5
                        {
                            "error_message": "エラーの内容。日付や金額が画像から見つからない場合に、ユーザーに入力を促すメッセージ等(String?)",
                            "results": [
                                {
                                    "date": "日付、時間の ISO8601 Offsetなし (例: 2025-01-20T12:00:00)(String)",
                                    "amount": "合計金額(税込み)。日本円の金額、ドル表記であればドルで良い(Int)",
                                    "title": "店名(String)",
                                    "description": "商品名一覧(String?)",
                                },
                                /* 以下商品の配列 */
                                {
                                    "date": "日付、時間の ISO8601 Offsetなし (例: 2025-01-20T12:00:00)(String)",
                                    "amount": "1つの商品の金額(同じものが2点あれば合計する)。税込みにする。日本円の金額、ドル表記であればドルで良い(Int)",
                                    "title": "商品名(String)",
                                    "description": "補足情報(String?)"
                                },
                            ]
                        }
                        ```
                    """.trimIndent(),
                    format = ChatGptClientInterface.Format.Json,
                    responseTransformer = { TextMessageComposableInterface(MoneyResponseParser().toAnnotatedString(it)) },
                    model = ChatGptModel.GeminiFlashLatest,
                    summaryProvider = {
                        val parsed = MoneyResponseParser().parse(it)
                        parsed?.results?.lastOrNull()?.title ?: parsed?.errorMessage
                    },
                )
            }

            BuiltinProjectId.Emoji -> {
                Info(
                    systemMessage = """
                        与えられたテキストにマッチした絵文字の候補を10個あげてください。
                        以下のJSONフォーマットに従ってください。
                        ```json5
                        {
                            "results": ["emoji1", "emoji2", ...],
                        }
                        ```
                    """.trimIndent(),
                    format = ChatGptClientInterface.Format.Json,
                    responseTransformer = {
                        EmojiResponseParser().getEmojiList(it) { emoji ->
                            platformRequest.copyToClipboard(emoji)
                        }
                    },
                    model = ChatGptModel.GeminiFlashLatest,
                    summaryProvider = { it },
                )
            }

            else -> throw NotImplementedError()
        }
    }

    data class Info(
        val systemMessage: String,
        val format: ChatGptClientInterface.Format,
        val responseTransformer: (String) -> ChatMessageComposableInterface,
        val summaryProvider: (String) -> String?,
        val model: ChatGptModel,
    )
}
