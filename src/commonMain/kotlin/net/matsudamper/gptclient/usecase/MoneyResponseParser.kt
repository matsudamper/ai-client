package net.matsudamper.gptclient.usecase

import java.time.format.DateTimeFormatterBuilder
import kotlinx.serialization.json.Json
import io.ktor.http.encodeURLParameter
import net.matsudamper.gptclient.ui.jsonui.UiNode
import net.matsudamper.gptclient.viewmodel.calendar.MoneyGptResponse

class MoneyResponseParser {
    fun parse(original: String): MoneyGptResponse? = try {
        Json.decodeFromString<MoneyGptResponse>(original)
    } catch (e: Throwable) {
        e.printStackTrace()
        null
    }

    fun toUiNode(original: String): UiNode = try {
        val response = Json.decodeFromString<MoneyGptResponse>(original)
        if (response.results.isEmpty()) {
            UiNode.Txt(v = response.errorMessage ?: original)
        } else {
            UiNode.Col(c = buildList {
                for ((index, result) in response.results.withIndex()) {
                    val dateTime = DateTimeFormatterBuilder()
                        .appendPattern("yyyy-MM-dd HH:mm")
                        .toFormatter()
                        .format(result.date)

                    add(UiNode.Txt(v = result.title, s = "h"))
                    add(UiNode.Kv(k = "日時", v = dateTime))
                    add(UiNode.Kv(k = "金額", v = result.amount.toString()))
                    if (result.description != null) {
                        add(UiNode.Kv(k = "説明", v = result.description))
                    }
                    val url = "https://money.matsudamper.net/add/money-usage" +
                        "?action=TEMPLATE" +
                        "&title=${result.title.encodeURLParameter()}" +
                        "&date=${result.date}" +
                        "&price=${result.amount}" +
                        "&description=${result.description.orEmpty().encodeURLParameter()}"
                    add(UiNode.Lnk(v = "家計簿への追加リンク", u = url))
                    if (index < response.results.size - 1) {
                        add(UiNode.Div)
                    }
                }
            })
        }
    } catch (e: Throwable) {
        e.printStackTrace()
        UiNode.Txt(v = original)
    }
}
