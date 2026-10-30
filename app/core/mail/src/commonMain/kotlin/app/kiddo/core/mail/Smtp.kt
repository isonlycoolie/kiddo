package app.kiddo.core.mail

import app.kiddo.core.model.Failure
import app.kiddo.core.model.MailSecurity
import app.kiddo.core.model.MailServer
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

// A minimal SMTP client: EHLO, STARTTLS when asked, AUTH PLAIN, then one message. Never authenticates in the clear.
internal class Smtp(private val server: MailServer) {
    fun login(user: String, password: String): SmtpSession {
        val socket = connectMail(server.host, server.port, tls = server.security == MailSecurity.SslTls)
        try {
            expect(socket, READY)
            ehlo(socket)
            if (server.security == MailSecurity.StartTls) {
                socket.write("STARTTLS")
                expect(socket, READY)
                socket.upgrade()
                ehlo(socket)
            }
            val token = plainToken(user, password)
            socket.write("AUTH PLAIN $token")
            val (code, text) = reply(socket)
            if (code != AUTH_OK) throw SmtpRefused(authFailure(code, text))
            return SmtpSession(socket)
        } catch (e: Exception) {
            socket.close()
            throw e
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun plainToken(user: String, password: String) = Base64.Default.encode("\u0000$user\u0000$password".encodeToByteArray())

    private fun ehlo(socket: MailSocket) {
        socket.write("EHLO kiddo.local")
        expect(socket, OK)
    }

    // Gmail answers 534 when the account needs an app password, 535 for a wrong one.
    private fun authFailure(code: Int, text: String): Failure = when {
        code == NEEDS_APP_PASSWORD || text.contains("Application-specific", ignoreCase = true) -> Failure.AppPasswordNeeded
        code == BAD_CREDENTIALS -> Failure.WrongPassword
        else -> Failure.WrongPassword
    }

    companion object {
        const val READY = 220
        const val OK = 250
        const val AUTH_OK = 235
        const val DATA_GO = 354
        const val NEEDS_APP_PASSWORD = 534
        const val BAD_CREDENTIALS = 535

        // Reads one reply, joining multi-line answers ("250-...", then "250 ...").
        fun reply(socket: MailSocket): Pair<Int, String> {
            val text = StringBuilder()
            while (true) {
                val line = socket.line()
                text.appendLine(line)
                if (line.length < 4 || line[3] != '-') {
                    return (line.take(3).toIntOrNull() ?: 0) to text.toString()
                }
            }
        }

        fun expect(socket: MailSocket, code: Int) {
            val (got, text) = reply(socket)
            if (got != code) throw MailProtocolException("Expected $code, got $got: ${text.take(120)}")
        }
    }
}

internal class SmtpSession(private val socket: MailSocket) : AutoCloseable {
    fun send(from: String, to: List<String>, message: ByteArray) {
        socket.write("MAIL FROM:<$from>")
        Smtp.expect(socket, Smtp.OK)
        to.forEach {
            socket.write("RCPT TO:<$it>")
            Smtp.expect(socket, Smtp.OK)
        }
        socket.write("DATA")
        Smtp.expect(socket, Smtp.DATA_GO)
        socket.writeRaw(message)
        socket.write("\r\n.")
        Smtp.expect(socket, Smtp.OK)
    }

    override fun close() {
        runCatching { socket.write("QUIT") }
        socket.close()
    }
}

internal class SmtpRefused(val failure: Failure) : Exception(failure.name)
