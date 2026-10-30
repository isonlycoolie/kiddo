package app.kiddo.core.web

import app.kiddo.core.model.Failure
import app.kiddo.core.model.Outcome
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException

// A job page reduced to what a person reads: title, plain text, and any email addresses on it.
data class Page(val url: String, val title: String, val text: String, val emails: List<String>) {
    // Pages built by scripts come back almost empty; those need the Kiddo browser.
    val needsBrowser: Boolean get() = text.length < MIN_TEXT
}

class PageReader(private val http: HttpClient) {
    suspend fun read(url: String): Outcome<Page> {
        if (!url.startsWith("https://") && !url.startsWith("http://")) return Outcome.Failed(Failure.BadInput)
        return try {
            val response = http.get(url) {
                header("Accept", "text/html,application/xhtml+xml")
                header("User-Agent", AGENT)
            }
            val status = response.status.value
            when {
                status == 404 -> Outcome.Failed(Failure.NotFound)
                status !in 200..299 -> Outcome.Failed(Failure.CantReach)
                else -> Outcome.Done(Html.page(url, response.bodyAsText().take(MAX_HTML)))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Outcome.Failed(Failure.CantReach)
        }
    }

    private companion object {
        const val MAX_HTML = 2_000_000
        const val AGENT = "Mozilla/5.0 (Linux; Android 14) Kiddo"
    }
}

// HTML to readable text: scripts, styles and navigation go, block tags become line breaks, entities are decoded.
object Html {
    fun page(url: String, html: String): Page {
        val title = Regex("<title[^>]*>(.*?)</title>", RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1)?.let(::decode).orEmpty()
        val text = text(html)
        return Page(url, title.trim(), text, emails(html + " " + text))
    }

    fun text(html: String): String = html
        .replace(Regex("(?is)<(script|style|noscript|svg|nav|footer|header)[^>]*>.*?</\\1>"), " ")
        .replace(Regex("(?i)<br\\s*/?>"), "\n")
        .replace(Regex("(?i)</(p|div|li|h[1-6]|tr|section|article)>"), "\n")
        .replace(Regex("(?i)<li[^>]*>"), "- ")
        .replace(Regex("<[^>]+>"), " ")
        .let(::decode)
        .lines()
        .map { it.replace(Regex("[ \\t\\u00A0]+"), " ").trim() }
        .filter { it.isNotEmpty() }
        .joinToString("\n")
        .take(MAX_TEXT)

    fun emails(text: String): List<String> =
        Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}").findAll(text).map { it.value.trimEnd('.') }.distinct().take(MAX_EMAILS).toList()

    private fun decode(s: String) = s
        .replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
        .replace("&quot;", "\"").replace("&#39;", "'").replace("&apos;", "'")
        .replace(Regex("&#(\\d+);")) { m -> m.groupValues[1].toIntOrNull()?.let { Char(it).toString() } ?: "" }
}

// Finds the first web address in what the user wrote.
fun firstUrl(text: String): String? = Regex("https?://[^\\s<>\"')]+").find(text)?.value?.trimEnd('.', ',')
    ?: Regex("\\b([a-z0-9-]+\\.)+[a-z]{2,}(/[^\\s<>\"')]*)?", RegexOption.IGNORE_CASE).find(text)?.value
        ?.takeIf { '/' in it && '@' !in it }
        ?.let { "https://$it" }

private const val MIN_TEXT = 400
private const val MAX_TEXT = 100_000
private const val MAX_EMAILS = 10
