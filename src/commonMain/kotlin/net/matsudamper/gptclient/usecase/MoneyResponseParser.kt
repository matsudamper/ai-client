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
            UiNode.Text(value = response.errorMessage ?: original)
        } else {
            UiNode.Column(children = buildList {
                for ((index, result) in response.results.withIndex()) {
                    val dateTime = DateTimeFormatterBuilder()
                        .appendPattern("yyyy-MM-dd HH:mm")
                        .toFormatter()
                        .format(result.date)

                    add(UiNode.Text(value = result.title, style = "h"))
                    add(UiNode.KeyValue(key = "日時", value = dateTime))
                    add(UiNode.KeyValue(key = "金額", value = result.amount.toString()))
                    if (result.description != null) {
                        add(UiNode.KeyValue(key = "説明", value = result.description))
                    }
                    val url = "https://money.matsudamper.net/add/money-usage" +
                        "?action=TEMPLATE" +
                        "&title=${result.title.encodeURLParameter()}" +
                        "&date=${result.date}" +
                        "&price=${result.amount}" +
                        "&description=${result.description.orEmpty().encodeURLParameter()}"
                    add(UiNode.Link(label = "家計簿への追加リンク", url = url))
                    if (index < response.results.size - 1) {
                        add(UiNode.Divider)
                    }
                }
            })
        }
    } catch (e: Throwable) {
        e.printStackTrace()
        UiNode.Text(value = original)
    }
}
