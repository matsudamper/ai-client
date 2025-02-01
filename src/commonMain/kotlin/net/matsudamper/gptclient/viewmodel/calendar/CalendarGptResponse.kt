package net.matsudamper.gptclient.viewmodel.calendar

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.matsudamper.gptclient.serialization.ISO8601LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
data class CalendarGptResponse(
    @SerialName("error_message") val errorMessage: String?,
    @SerialName("results") val results: List<Result>,
) {
    @Serializable
    data class Result(
        @Serializable(with =ISO8601LocalDateTimeSerializer::class)
        @SerialName("start_date") val startDate: LocalDateTime,
        @Serializable(with = ISO8601LocalDateTimeSerializer::class)
        @SerialName("end_date") val endDate: LocalDateTime,
        @SerialName("title") val title: String,
        @SerialName("description") val description: String?,
        @SerialName("location") val location: String?,
    )
}
