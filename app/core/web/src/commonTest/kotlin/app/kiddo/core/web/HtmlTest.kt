package app.kiddo.core.web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HtmlTest {
    @Test
    fun keepsReadableTextAndDropsScripts() {
        val page = Html.page(
            "https://jobs.example.com/designer",
            "<html><title>Designer &amp; Co</title><script>alert(1)</script><h1>Product Designer</h1>" +
                "<p>Send your CV to careers@example.com.</p><ul><li>Figma</li></ul></html>",
        )
        assertEquals("Designer & Co", page.title)
        assertTrue("Product Designer" in page.text)
        assertTrue("- Figma" in page.text)
        assertFalse("alert" in page.text)
        assertEquals(listOf("careers@example.com"), page.emails)
    }

    @Test
    fun findsLinksWithOrWithoutScheme() {
        assertEquals("https://safaripay.co.tz/careers/designer", firstUrl("Saw this: safaripay.co.tz/careers/designer today"))
        assertEquals("https://a.io/x", firstUrl("see https://a.io/x."))
        assertEquals(null, firstUrl("email me at me@a.io"))
    }
}
