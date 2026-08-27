package net.matsudamper.gptclient.localmodel

import com.google.mlkit.genai.schema.annotations.Generable
import com.google.mlkit.genai.schema.annotations.Guide
import org.json.JSONArray
import org.json.JSONObject

interface JsonOutput {
    fun toJson(): String
}

@Generable("カレンダーへ登録する予定の抽出結果")
data class CalendarOutput(
    @Guide(description = "必要な情報がない場合の質問。問題がなければnull") val errorMessage: String?,
    @Guide(description = "抽出した予定", minItems = 0, maxItems = 20) val results: List<CalendarItem>,
) : JsonOutput {
    override fun toJson(): String = JSONObject()
        .put("error_message", errorMessage ?: JSONObject.NULL)
        .put("results", JSONArray(results.map(CalendarItem::toJsonObject)))
        .toString()
}

@Generable("カレンダーへ登録する予定")
data class CalendarItem(
    @Guide(description = "開始日時。OffsetなしのISO 8601形式") val startDate: String,
    @Guide(description = "終了日時。OffsetなしのISO 8601形式") val endDate: String,
    @Guide(description = "予定のタイトル") val title: String,
    @Guide(description = "場所。情報がなければnull") val location: String?,
    @Guide(description = "補足情報。情報がなければnull") val description: String?,
) {
    fun toJsonObject(): JSONObject = JSONObject()
        .put("start_date", startDate)
        .put("end_date", endDate)
        .put("title", title)
        .put("location", location ?: JSONObject.NULL)
        .put("description", description ?: JSONObject.NULL)
}

@Generable("家計簿へ登録する支出の抽出結果")
data class MoneyOutput(
    @Guide(description = "必要な情報がない場合の質問。問題がなければnull") val errorMessage: String?,
    @Guide(description = "抽出した支出", minItems = 0, maxItems = 30) val results: List<MoneyItem>,
) : JsonOutput {
    override fun toJson(): String = JSONObject()
        .put("error_message", errorMessage ?: JSONObject.NULL)
        .put("results", JSONArray(results.map(MoneyItem::toJsonObject)))
        .toString()
}

@Generable("家計簿へ登録する支出")
data class MoneyItem(
    @Guide(description = "日時。OffsetなしのISO 8601形式") val date: String,
    @Guide(description = "税込み金額", minimum = 0.0) val amount: Int,
    @Guide(description = "店名または商品名") val title: String,
    @Guide(description = "補足情報。情報がなければnull") val description: String?,
) {
    fun toJsonObject(): JSONObject = JSONObject()
        .put("date", date)
        .put("amount", amount)
        .put("title", title)
        .put("description", description ?: JSONObject.NULL)
}

@Generable("入力に合う絵文字の候補")
data class EmojiOutput(
    @Guide(description = "絵文字のみの候補", minItems = 10, maxItems = 10) val results: List<String>,
) : JsonOutput {
    override fun toJson(): String = JSONObject().put("results", JSONArray(results)).toString()
}
