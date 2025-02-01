package net.matsudamper.gptclient.viewmodel.calendar

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import net.matsudamper.gptclient.serialization.InstantSerializer
import java.time.Instant

@Serializable
data class CalendarGptResponse(
    @SerialName("error_message") val errorMessage: String?,
    @SerialName("results") val results: List<Result>,
) {
    @Serializable
    data class Result(
        @Serializable(with = InstantSerializer::class)
        @SerialName("start_date") val startDate: Instant,
        @Serializable(with = InstantSerializer::class)
        @SerialName("end_date") val endDate: Instant,
        @SerialName("title") val title: String,
        @SerialName("description") val description: String?,
        @SerialName("location") val location: String?,
    )
}
