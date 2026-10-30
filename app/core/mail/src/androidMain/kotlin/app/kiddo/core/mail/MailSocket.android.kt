package app.kiddo.core.mail

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

internal actual fun connectMail(host: String, port: Int, tls: Boolean): MailSocket = net {
    val raw = if (tls) (SSLSocketFactory.getDefault() as SSLSocketFactory).createSocket() else Socket()
    raw.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT)
    raw.soTimeout = READ_TIMEOUT
    JvmMailSocket(if (raw is SSLSocket) verified(raw, host) else raw, host)
}

// The default socket factory doesn't check host names, so the platform's HTTPS verifier runs after every handshake.
private class JvmMailSocket(private var socket: Socket, private val host: String) : MailSocket {
    private var reader = reader(socket)
    private var out: OutputStream = socket.getOutputStream()

    override fun line(): String = net { reader.readLine() } ?: throw MailNetworkException(tls = false)

    override fun write(text: String) = net {
        out.write(text.encodeToByteArray())
        out.write(CRLF)
        out.flush()
    }

    override fun writeRaw(bytes: ByteArray) = net {
        out.write(bytes)
        out.flush()
    }

    override fun upgrade() = net {
        val tls = (SSLSocketFactory.getDefault() as SSLSocketFactory).createSocket(socket, host, socket.port, true)
        socket = verified(tls as SSLSocket, host)
        reader = reader(socket)
        out = socket.getOutputStream()
    }

    override fun close() {
        runCatching { socket.close() }
    }
}

private fun verified(socket: SSLSocket, host: String): SSLSocket {
    socket.startHandshake()
    if (!HttpsURLConnection.getDefaultHostnameVerifier().verify(host, socket.session)) {
        socket.close()
        throw MailNetworkException(tls = true)
    }
    return socket
}

private fun reader(socket: Socket) = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))

private inline fun <T> net(block: () -> T): T = try {
    block()
} catch (e: SSLException) {
    throw MailNetworkException(tls = true)
} catch (e: IOException) {
    throw MailNetworkException(tls = false)
}

private const val CONNECT_TIMEOUT = 10_000
private const val READ_TIMEOUT = 30_000
