package net.matsudamper.gptclient.usecase

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoField
import kotlinx.serialization.json.Json
import io.ktor.http.encodeURLParameter
import net.matsudamper.gptclient.ui.jsonui.UiNode
import net.matsudamper.gptclient.viewmodel.calendar.CalendarGptResponse

class CalendarResponseParser {
    fun parse(original: String): CalendarGptResponse? = try {
        Json.decodeFromString<CalendarGptResponse>(original)
    } catch (e: Throwable) {
        e.printStackTrace()
        null
    }

    fun toUiNode(original: String): UiNode = try {
        val response = Json.decodeFromString<CalendarGptResponse>(original)
        if (response.results.isEmpty()) {
            UiNode.Text(value = response.errorMessage ?: original)
        } else {
            UiNode.Column(children = buildList {
                for ((index, result) in response.results.withIndex()) {
                    add(UiNode.Text(value = result.title))
                    add(UiNode.KeyValue(key = "日時", value = "${result.startDate.toDisplayFormat()}~${result.endDate.toDisplayFormat()}"))
                    add(UiNode.KeyValue(key = "場所", value = result.location.toString()))
                    add(UiNode.KeyValue(key = "説明", value = result.description.toString()))
                    val googleCalendarUrl = "https://calendar.google.com/calendar/render" +
                        "?action=TEMPLATE" +
                        "&text=${result.title.encodeURLParameter()}" +
                        "&dates=${result.startDate.toGoogleCalendarFormat()}/${result.endDate.toGoogleCalendarFormat()}" +
                        "&details=${result.description.orEmpty().encodeURLParameter()}" +
                        "&location=${result.location.orEmpty().encodeURLParameter()}"
                    add(UiNode.Link(label = "Google Calendar追加リンク", url = googleCalendarUrl))
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

    private fun LocalDateTime.toGoogleCalendarFormat(): String {
        with(
            atZone(ZoneId.systemDefault()).toOffsetDateTime()
                .withOffsetSameInstant(ZoneOffset.UTC),
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
            return "$year/$month/$dayOfMonth $hour:$minute"
        }
    }
}
