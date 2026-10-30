package app.kiddo.core.mail

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MailDatesTest {
    @Test
    fun writesUtcDateHeader() {
        assertEquals("Thu, 1 Jan 1970 00:00:00 +0000", mailDate(0))
        assertEquals("Fri, 30 Oct 2026 08:58:00 +0000", mailDate(OCT_30))
        assertEquals("Fri, 31 Dec 1999 23:59:59 +0000", mailDate(Y2K_EVE))
    }

    @Test
    fun writesImapSearchDate() {
        assertEquals("30-Oct-2026", imapDate(OCT_30))
    }

    @Test
    fun readsZonesAndVariants() {
        assertEquals(OCT_30, parseMailDate("Fri, 30 Oct 2026 08:58:00 +0000"))
        assertEquals(OCT_30, parseMailDate("Fri, 30 Oct 2026 11:58:00 +0300 (EAT)"))
        assertEquals(OCT_30 - THREE_HOURS, parseMailDate("30 Oct 2026 08:58:00 +0300"))
        assertEquals(OCT_30, parseMailDate("Fri, 30 Oct 2026 08:58 GMT"))
        assertEquals(Y2K_EVE, parseMailDate("31 Dec 99 23:59:59 +0000"))
    }

    @Test
    fun roundTrips() {
        assertEquals(OCT_30, parseMailDate(mailDate(OCT_30)))
    }

    @Test
    fun refusesNonsense() {
        assertNull(parseMailDate(""))
        assertNull(parseMailDate("yesterday"))
        assertNull(parseMailDate("Fri, 30 Foo 2026 08:58:00 +0000"))
    }

    private companion object {
        const val OCT_30 = 1_793_350_680_000L
        const val Y2K_EVE = 946_684_799_000L
        const val THREE_HOURS = 3 * 3_600_000L
    }
}
