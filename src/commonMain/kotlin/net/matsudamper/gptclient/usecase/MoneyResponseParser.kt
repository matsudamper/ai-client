package net.matsudamper.gptclient.usecase

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.json.Json
import net.matsudamper.gptclient.viewmodel.calendar.MoneyGptResponse
import java.time.format.DateTimeFormatterBuilder

class MoneyResponseParser() {
    fun parse(original: String): AnnotatedString {
        return try {
            val response = Json.decodeFromString<MoneyGptResponse>(original)
            if (response.results.isEmpty()) {
                AnnotatedString(response.errorMessage ?: original)
            } else {
                buildAnnotatedString {
                    for ((index, result) in response.results.withIndex()) {
                        val date = DateTimeFormatterBuilder()
                            .appendPattern("yyyy-MM-dd")
                            .toFormatter()
                            .format(result.date)

                        appendLine(result.title)
                        appendLine("タイトル: ${result.title}")
                        appendLine("日付: ${date}")
                        appendLine("金額: ${result.amount}")
                        appendLine("説明: ${result.description}")

                        val googleCalendarUrl = "https://money.matsudamper.net/add/money-usage" +
                                "?action=TEMPLATE" +
                                "&title=${result.title.encodeURLParameter()}" +
                                "&date=${date}" +
                                "&price=${result.description.orEmpty().encodeURLParameter()}"
                        pushLink(LinkAnnotation.Url(googleCalendarUrl))
                        withStyle(SpanStyle(color = Color.Blue)) {
                            append("Google Calendar追加リンク")
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
}
