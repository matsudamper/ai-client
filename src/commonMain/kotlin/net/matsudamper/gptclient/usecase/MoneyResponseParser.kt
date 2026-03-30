package net.matsudamper.gptclient.usecase

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import java.time.format.DateTimeFormatterBuilder
import kotlinx.serialization.json.Json
import io.ktor.http.encodeURLParameter
import net.matsudamper.gptclient.viewmodel.calendar.MoneyGptResponse

class MoneyResponseParser {
    fun parse(original: String): MoneyGptResponse? = try {
        Json.decodeFromString<MoneyGptResponse>(original)
    } catch (e: Throwable) {
        e.printStackTrace()
        null
    }

    fun toAnnotatedString(original: String): AnnotatedString = try {
        val response = Json.decodeFromString<MoneyGptResponse>(original)
        if (response.results.isEmpty()) {
            AnnotatedString(response.errorMessage ?: original)
        } else {
            buildAnnotatedString {
                for ((index, result) in response.results.withIndex()) {
                    val dateTime = DateTimeFormatterBuilder()
                        .appendPattern("yyyy-MM-dd HH:mm")
                        .toFormatter()
                        .format(result.date)

                    appendLine("タイトル: ${result.title}")
                    appendLine("日時: $dateTime")
                    appendLine("金額: ${result.amount}")
                    appendLine("説明: ${result.description}")

                    val googleCalendarUrl = "https://money.matsudamper.net/add/money-usage" +
                        "?action=TEMPLATE" +
                        "&title=${result.title.encodeURLParameter()}" +
                        "&date=${result.date}" +
                        "&price=${result.amount}" +
                        "&description=${result.description.orEmpty().encodeURLParameter()}"
                    pushLink(LinkAnnotation.Url(googleCalendarUrl))
                    withStyle(SpanStyle(color = Color.Blue)) {
                        append("家計簿への追加リンク")
                    }
                    pop()
                    if (index < response.results.size - 1) {
                        appendLine()
                        appendLine()
                    }
                }
            }
        }
    } catch (e: Throwable) {
        e.printStackTrace()
        AnnotatedString(original)
    }
}
