package app.kiddo.core.mail

import app.kiddo.core.model.OutgoingMail
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// Builds the RFC 5322 message: a UTF-8 plain-text body and, when there is one, the CV as a base64 attachment.
// Every line is base64 or a header, so no line can start with "." and dot-stuffing is never needed.
@OptIn(ExperimentalEncodingApi::class, ExperimentalUuidApi::class)
internal object Mime {
    // The message and the Message-ID it carries.
    fun build(mail: OutgoingMail, domain: String, now: Long): Pair<ByteArray, String> {
        val boundary = "kiddo-${Uuid.random()}"
        val messageId = "<${Uuid.random()}@$domain>"
        val thread = mail.inReplyTo?.let { listOf("In-Reply-To: $it", "References: $it") }.orEmpty()
        val headers = listOf(
            "From: ${word(mail.fromName)} <${mail.from}>",
            "To: ${mail.to.joinToString(", ")}",
            "Subject: ${word(mail.subject)}",
            "Date: ${mailDate(now)}",
            "Message-ID: $messageId",
            "MIME-Version: 1.0",
        ) + thread
        val body = base64Lines(mail.body.replace("\r\n", "\n").replace("\n", "\r\n").encodeToByteArray())
        val text = mail.attachment?.let { file ->
            listOf(
                "Content-Type: multipart/mixed; boundary=\"$boundary\"",
                "",
                "--$boundary",
                "Content-Type: text/plain; charset=UTF-8",
                "Content-Transfer-Encoding: base64",
                "",
                body,
                "--$boundary",
                "Content-Type: ${file.mimeType}; name=\"${file.fileName}\"",
                "Content-Disposition: attachment; filename=\"${file.fileName}\"",
                "Content-Transfer-Encoding: base64",
                "",
                base64Lines(file.bytes),
                "--$boundary--",
            )
        } ?: listOf("Content-Type: text/plain; charset=UTF-8", "Content-Transfer-Encoding: base64", "", body)
        return (headers + text).joinToString("\r\n").encodeToByteArray() to messageId
    }

    // RFC 2047 encoded word when a header holds anything beyond plain ASCII.
    private fun word(text: String): String =
        if (text.all { it.code in PRINTABLE }) {
            text
        } else {
            "=?UTF-8?B?${Base64.Default.encode(text.encodeToByteArray())}?="
        }

    // 76-character lines joined by CRLF, as MIME asks.
    private fun base64Lines(bytes: ByteArray): String = Base64.Mime.encode(bytes)

    private val PRINTABLE = 32..126
}
