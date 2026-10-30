package app.kiddo.core.platform

import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.localTimeZone

actual fun localDay(epochMillis: Long): Long {
    val offset = NSTimeZone.localTimeZone.secondsFromGMTForDate(date(epochMillis)) * 1000L
    val local = epochMillis + offset
    return if (local >= 0) local / DAY else -((-local + DAY - 1) / DAY)
}

// "24 Oct" ordered and spelled as the phone's region writes it.
actual fun shortDate(epochMillis: Long): String {
    val format = NSDateFormatter()
    format.setLocalizedDateFormatFromTemplate("dMMM")
    return format.stringFromDate(date(epochMillis))
}

// The phone's own calendar decides which days are the weekend.
actual fun isWeekend(epochMillis: Long): Boolean = NSCalendar.currentCalendar.isDateInWeekend(date(epochMillis))

private fun date(epochMillis: Long) = NSDate.dateWithTimeIntervalSince1970(epochMillis / 1000.0)

private const val DAY = 86_400_000L
