package app.kiddo.core.mail

// Mail dates without a platform calendar: written in UTC, read from any RFC 5322 zone.

private val Days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
private val Months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
private const val DAY_MS = 86_400_000L
private const val MINUTE_MS = 60_000L

// "Thu, 30 Oct 2026 08:58:00 +0000" for the Date header.
internal fun mailDate(epochMillis: Long): String {
    val days = floorDiv(epochMillis, DAY_MS)
    val (y, m, d) = civil(days)
    val inDay = epochMillis - days * DAY_MS
    val h = inDay / 3_600_000
    val min = inDay / MINUTE_MS % 60
    val s = inDay / 1000 % 60
    return "${Days[floorMod(days + 4, 7).toInt()]}, $d ${Months[m - 1]} $y ${two(h)}:${two(min)}:${two(s)} +0000"
}

// "30-Oct-2026" for IMAP SEARCH SINCE, which compares dates only.
internal fun imapDate(epochMillis: Long): String {
    val (y, m, d) = civil(floorDiv(epochMillis, DAY_MS))
    return "$d-${Months[m - 1]}-$y"
}

// Reads "Thu, 30 Oct 2026 08:58:00 +0300 (EAT)" and the variants servers send; null when it isn't a date.
internal fun parseMailDate(text: String): Long? {
    val parts = text.substringAfter(',', text).trim().split(Regex("\\s+"))
    if (parts.size < 4) return null
    val d = parts[0].toIntOrNull() ?: return null
    val m = Months.indexOfFirst { it.equals(parts[1].take(3), ignoreCase = true) } + 1
    if (m == 0) return null
    val y = parts[2].toIntOrNull()?.let { if (it < 50) it + 2000 else if (it < 100) it + 1900 else it } ?: return null
    val clock = parts[3].split(':').map { it.toIntOrNull() ?: return null }
    if (clock.size < 2) return null
    val offset = parts.getOrNull(4)?.let(::zoneMinutes) ?: 0
    val minutes = clock[0] * 60L + clock[1] - offset
    return daysFrom(y, m, d) * DAY_MS + minutes * MINUTE_MS + (clock.getOrNull(2) ?: 0) * 1000L
}

private fun zoneMinutes(zone: String): Int {
    if (zone.length != 5 || (zone[0] != '+' && zone[0] != '-')) return 0
    val hh = zone.substring(1, 3).toIntOrNull() ?: return 0
    val mm = zone.substring(3, 5).toIntOrNull() ?: return 0
    return (if (zone[0] == '-') -1 else 1) * (hh * 60 + mm)
}

private fun two(n: Long) = n.toString().padStart(2, '0')

private fun floorDiv(a: Long, b: Long): Long = if (a >= 0) a / b else -((-a + b - 1) / b)

private fun floorMod(a: Long, b: Long): Long = a - floorDiv(a, b) * b

// Days since 1970-01-01 to year, month, day in the proleptic Gregorian calendar, and back.
private fun civil(days: Long): Triple<Int, Int, Int> {
    val z = days + 719_468
    val era = floorDiv(z, 146_097)
    val doe = z - era * 146_097
    val yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = doy - (153 * mp + 2) / 5 + 1
    val m = if (mp < 10) mp + 3 else mp - 9
    val y = yoe + era * 400 + if (m <= 2) 1 else 0
    return Triple(y.toInt(), m.toInt(), d.toInt())
}

private fun daysFrom(year: Int, month: Int, day: Int): Long {
    val y = (if (month <= 2) year - 1 else year).toLong()
    val era = floorDiv(y, 400)
    val yoe = y - era * 400
    val mp = (month + 9) % 12
    val doy = (153 * mp + 2) / 5 + day - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146_097 + doe - 719_468
}
