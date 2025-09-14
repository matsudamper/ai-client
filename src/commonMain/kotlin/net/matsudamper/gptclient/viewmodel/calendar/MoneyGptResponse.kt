package net.matsudamper.gptclient.viewmodel.calendar

import java.time.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.matsudamper.gptclient.serialization.ISO8601LocalDateTimeSerializer

@Serializable
data class MoneyGptResponse(
    @SerialName("error_message") val errorMessage: String?,
    @SerialName("results") val results: List<Result>,
) {
    @Serializable
    data class Result(
        @Serializable(with = ISO8601LocalDateTimeSerializer::class)
        @SerialName("date") val date: LocalDateTime,
        @SerialName("amount") val amount: Int,
        @SerialName("title") val title: String,
        @SerialName("description") val description: String?,
    )
}
