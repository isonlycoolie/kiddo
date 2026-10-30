package app.kiddo.core.mail

// A line-based connection to a mail server. Each platform opens it with its own TLS, always checking the certificate
// against the host name.
internal interface MailSocket : AutoCloseable {
    fun line(): String

    fun write(text: String)

    fun writeRaw(bytes: ByteArray)

    // STARTTLS: wraps the open plain connection in TLS for the same host.
    fun upgrade()
}

// `tls` opens straight into TLS (465, 993); otherwise the connection starts plain and must be upgraded before login.
internal expect fun connectMail(host: String, port: Int, tls: Boolean): MailSocket

internal class MailProtocolException(message: String) : Exception(message)

// The connection failed or dropped; `tls` when the secure handshake or the certificate was the problem.
internal class MailNetworkException(val tls: Boolean) : Exception(if (tls) "TLS failed" else "Connection failed")

internal val CRLF = "\r\n".encodeToByteArray()
