package app.kiddo.core.platform

import java.util.TimeZone

actual fun localDay(epochMillis: Long): Long {
    val local = epochMillis + TimeZone.getDefault().getOffset(epochMillis)
    return Math.floorDiv(local, DAY)
}

private const val DAY = 86_400_000L

actual fun shortDate(epochMillis: Long): String =
    java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault()).format(java.util.Date(epochMillis))

actual fun isWeekend(epochMillis: Long): Boolean {
    val day = java.util.Calendar.getInstance().apply { timeInMillis = epochMillis }.get(java.util.Calendar.DAY_OF_WEEK)
    return day == java.util.Calendar.SATURDAY || day == java.util.Calendar.SUNDAY
}
