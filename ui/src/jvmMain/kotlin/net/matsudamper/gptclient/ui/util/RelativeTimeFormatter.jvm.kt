package net.matsudamper.gptclient.ui.util

import com.ibm.icu.text.RelativeDateTimeFormatter
import java.time.Duration
import java.time.Instant
import java.util.Locale

public actual fun formatRelativeTime(instant: Instant): String {
    val seconds = Duration.between(instant, Instant.now()).seconds.coerceAtLeast(0)
    val formatter = RelativeDateTimeFormatter.getInstance(Locale.getDefault())

    if (seconds < 60) {
        return formatter.format(RelativeDateTimeFormatter.Direction.PLAIN, RelativeDateTimeFormatter.AbsoluteUnit.NOW)
    }

    val (quantity, unit) = when {
        seconds < 60 * 60 -> (seconds / 60) to RelativeDateTimeFormatter.RelativeUnit.MINUTES
        seconds < 60 * 60 * 24 -> (seconds / (60 * 60)) to RelativeDateTimeFormatter.RelativeUnit.HOURS
        seconds < 60 * 60 * 24 * 7 -> (seconds / (60 * 60 * 24)) to RelativeDateTimeFormatter.RelativeUnit.DAYS
        seconds < 60 * 60 * 24 * 30 -> (seconds / (60 * 60 * 24 * 7)) to RelativeDateTimeFormatter.RelativeUnit.WEEKS
        seconds < 60 * 60 * 24 * 365 -> (seconds / (60 * 60 * 24 * 30)) to RelativeDateTimeFormatter.RelativeUnit.MONTHS
        else -> (seconds / (60 * 60 * 24 * 365)) to RelativeDateTimeFormatter.RelativeUnit.YEARS
    }

    return formatter.format(quantity.toDouble(), RelativeDateTimeFormatter.Direction.LAST, unit)
}
