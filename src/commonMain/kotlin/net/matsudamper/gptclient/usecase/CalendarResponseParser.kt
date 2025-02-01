package net.matsudamper.gptclient.usecase

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.json.Json
import net.matsudamper.gptclient.viewmodel.calendar.CalendarGptResponse
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoField

class CalendarResponseParser() {
    fun parse(original: String): AnnotatedString {
        return try {
            val response = Json.decodeFromString<CalendarGptResponse>(original)
            if (response.results.isEmpty()) {
                AnnotatedString(response.errorMessage ?: original)
            } else {
                buildAnnotatedString {
                    for ((index, result) in response.results.withIndex()) {
                        appendLine(result.title)
                        appendLine("日時: ${result.startDate.toDisplayFormat()}~${result.endDate.toDisplayFormat()}")
                        appendLine("場所: ${result.location}")
                        appendLine("説明: ${result.description}")

                        val googleCalendarUrl = "https://calendar.google.com/calendar/render" +
                                "?action=TEMPLATE" +
                                "&text=${result.title.encodeURLParameter()}" +
                                "&dates=${result.startDate.toGoogleCalendarFormat()}/${result.endDate.toGoogleCalendarFormat()}" +
                                "&details=${
                                    result.description.orEmpty().encodeURLParameter()
                                }" +
                                "&location=${result.location.orEmpty().encodeURLParameter()}"
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

    private fun LocalDateTime.toGoogleCalendarFormat(): String {
        with(
            atZone(ZoneId.systemDefault()).toOffsetDateTime()
                .withOffsetSameInstant(ZoneOffset.UTC)
        ) {
            val year = get(ChronoField.YEAR)
            val month = get(ChronoField.MONTH_OF_YEAR).toString().padStart(2, '0')
            val dayOfMonth = get(ChronoField.DAY_OF_MONTH).toString().padStart(2, '0')
            val hour = get(ChronoField.HOUR_OF_DAY).toString().padStart(2, '0')
            val minute = get(ChronoField.MINUTE_OF_HOUR).toString().padStart(2, '0')
            return "$year$month${dayOfMonth}T${hour}${minute}00Z"
        }
    }

    private fun LocalDateTime.toDisplayFormat(): String {
        with(atZone(ZoneId.systemDefault()).toOffsetDateTime()) {
            val year = get(ChronoField.YEAR)
            val month = get(ChronoField.MONTH_OF_YEAR).toString().padStart(2, '0')
            val dayOfMonth = get(ChronoField.DAY_OF_MONTH).toString().padStart(2, '0')
            val hour = get(ChronoField.HOUR_OF_DAY).toString().padStart(2, '0')
            val minute = get(ChronoField.MINUTE_OF_HOUR).toString().padStart(2, '0')
            return "$year/$month/${dayOfMonth} ${hour}:${minute}"
        }
    }
}
