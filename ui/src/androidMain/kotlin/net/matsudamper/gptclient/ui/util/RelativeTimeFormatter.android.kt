package net.matsudamper.gptclient.ui.util

import android.text.format.DateUtils
import java.time.Instant

public actual fun formatRelativeTime(instant: Instant): String {
    return DateUtils.getRelativeTimeSpanString(
        instant.toEpochMilli(),
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
    ).toString()
}
