package app.kiddo.core.web

import app.kiddo.core.model.Failure
import app.kiddo.core.model.Outcome
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PageReaderTest {
    private fun reader(status: HttpStatusCode, body: String = "") = PageReader(
        HttpClient(MockEngine { respond(body, status, headersOf(HttpHeaders.ContentType, "text/html")) }),
    )

    @Test
    fun readsAPage() = runTest {
        val html = "<title>Designer</title><p>Apply via <a href=\"mailto:jobs@example.com?subject=CV\">email</a></p>"
        val page = (reader(HttpStatusCode.OK, html).read("https://example.com/job") as Outcome.Done).value
        assertEquals("Designer", page.title)
        assertEquals(listOf("jobs@example.com"), page.emails)
        assertTrue(page.needsBrowser)
    }

    @Test
    fun saysWhenThePageIsGone() = runTest {
        assertEquals(Outcome.Failed(Failure.NotFound), reader(HttpStatusCode.NotFound).read("https://example.com/x"))
        assertEquals(Outcome.Failed(Failure.CantReach), reader(HttpStatusCode.BadGateway).read("https://example.com/x"))
        assertEquals(Outcome.Failed(Failure.BadInput), reader(HttpStatusCode.OK).read("ftp://example.com"))
    }
}
