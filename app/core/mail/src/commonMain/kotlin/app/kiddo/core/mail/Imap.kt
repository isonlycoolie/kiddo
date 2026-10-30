package app.kiddo.core.mail

import app.kiddo.core.model.Failure
import app.kiddo.core.model.FoundReply
import app.kiddo.core.model.MailServer

// Enough IMAP for Kiddo: prove the inbox can be read (LOGIN), and list recent mail as headers plus a short start of
// the text, read-only (EXAMINE, BODY.PEEK), so nothing is marked read or changed in the user's mailbox.
internal class Imap(private val server: MailServer) {
    fun check(user: String, password: String): Failure? = connectMail(server.host, server.port, tls = true).use { socket ->
        if (!socket.line().startsWith("* OK")) return@use Failure.CantReach
        val answer = login(socket, user, password)
        runCatching { socket.write("a9 LOGOUT") }
        answer
    }

    fun recent(user: String, password: String, since: Long): List<FoundReply> = connectMail(server.host, server.port, tls = true).use { socket ->
        if (!socket.line().startsWith("* OK")) throw MailProtocolException("No greeting")
        login(socket, user, password)?.let { throw SmtpRefused(it) }
        socket.write("a2 EXAMINE INBOX")
        untilTagged(socket, "a2")
        socket.write("a3 SEARCH SINCE ${imapDate(since)}")
        val ids = untilTagged(socket, "a3").filter { it.startsWith("* SEARCH") }
            .flatMap { it.removePrefix("* SEARCH").trim().split(' ') }
            .filter { it.isNotBlank() }
            .takeLast(MAX_MESSAGES)
        val found = ids.mapIndexedNotNull { i, id -> fetch(socket, "f$i", id) }
        runCatching { socket.write("a9 LOGOUT") }
        found
    }

    private fun login(socket: MailSocket, user: String, password: String): Failure? {
        socket.write("a1 LOGIN ${quote(user)} ${quote(password)}")
        val answer = untilTagged(socket, "a1").last()
        return when {
            answer.startsWith("a1 OK") -> null
            answer.contains("IMAP access is disabled", ignoreCase = true) -> Failure.ImapOff
            answer.contains("Application-specific", ignoreCase = true) -> Failure.AppPasswordNeeded
            else -> Failure.WrongPassword
        }
    }

    private fun fetch(socket: MailSocket, tag: String, id: String): FoundReply? {
        socket.write("$tag FETCH $id (BODY.PEEK[HEADER.FIELDS (FROM SUBJECT DATE IN-REPLY-TO REFERENCES)] BODY.PEEK[TEXT]<0.$PEEK>)")
        val lines = untilTagged(socket, tag)
        val headers = unfold(lines).associate { line ->
            line.substringBefore(':').trim().lowercase() to line.substringAfter(':', "").trim()
        }
        val from = headers["from"] ?: return null
        val textStart = lines.indexOfFirst { it.contains("BODY[TEXT]") }
        val snippet = if (textStart < 0) "" else plain(lines.drop(textStart + 1).dropLast(2))
        val at = parseMailDate(headers["date"].orEmpty())
        return FoundReply(
            from = from,
            subject = headers["subject"].orEmpty(),
            inReplyTo = listOfNotNull(headers["in-reply-to"], headers["references"]).joinToString(" "),
            snippet = snippet,
            at = at ?: 0L,
        )
    }

    // Header lines that wrap continue with leading space; join them back.
    private fun unfold(lines: List<String>): List<String> {
        val out = mutableListOf<String>()
        lines.forEach { line ->
            if ((line.startsWith(" ") || line.startsWith("\t")) && out.isNotEmpty()) out[out.lastIndex] += " " + line.trim()
            else if (line.contains(':')) out += line
        }
        return out
    }

    // The readable start of a body: drops MIME parts' own headers and boundaries, keeps short plain lines.
    private fun plain(lines: List<String>): String = lines
        .filterNot { it.startsWith("--") || it.startsWith("Content-") || it.isBlank() || it.startsWith(">") }
        .joinToString(" ")
        .replace(Regex("<[^>]+>"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(SNIPPET)

    private fun untilTagged(socket: MailSocket, tag: String): List<String> {
        val lines = mutableListOf<String>()
        while (true) {
            val line = socket.line()
            lines += line
            if (line.startsWith("$tag ")) return lines
        }
    }

    // IMAP quoted string: backslash and double quote are escaped.
    private fun quote(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private companion object {
        const val MAX_MESSAGES = 60
        const val PEEK = 1500
        const val SNIPPET = 280
    }
}
