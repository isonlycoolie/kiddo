package app.kiddo.core.mail

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.Foundation.NSDate
import platform.Foundation.NSInputStream
import platform.Foundation.NSOutputStream
import platform.Foundation.NSStream
import platform.Foundation.NSStreamSocketSecurityLevelKey
import platform.Foundation.NSStreamSocketSecurityLevelNegotiatedSSL
import platform.Foundation.NSStreamStatusAtEnd
import platform.Foundation.NSStreamStatusClosed
import platform.Foundation.NSStreamStatusError
import platform.Foundation.NSStreamStatusOpen
import platform.Foundation.getStreamsToHostWithName
import platform.Foundation.timeIntervalSince1970
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_time
import platform.posix.usleep
import kotlin.concurrent.AtomicInt

// A socket stream pair from Foundation. TLS is the system's: the certificate chain and the host name are checked by
// default, and STARTTLS switches the open streams to TLS in place. Blocking reads and writes run under a watchdog
// that closes the streams if the server goes quiet, so a check never hangs.
internal actual fun connectMail(host: String, port: Int, tls: Boolean): MailSocket {
    val (input, output) = streams(host, port)
    return StreamSocket(input, output, tls).also { it.open() }
}

@OptIn(ExperimentalForeignApi::class)
private fun streams(host: String, port: Int): Pair<NSInputStream, NSOutputStream> = memScoped {
    val input = alloc<ObjCObjectVar<NSInputStream?>>()
    val output = alloc<ObjCObjectVar<NSOutputStream?>>()
    NSStream.getStreamsToHostWithName(host, port.convert(), input.ptr, output.ptr)
    val i = input.value ?: throw MailNetworkException(tls = false)
    val o = output.value ?: throw MailNetworkException(tls = false)
    i to o
}

@OptIn(ExperimentalForeignApi::class)
private class StreamSocket(
    private val input: NSInputStream,
    private val output: NSOutputStream,
    private var secured: Boolean,
) : MailSocket {
    private var buffer = ByteArray(0)
    private val chunk = ByteArray(CHUNK)
    private val watch = AtomicInt(0)

    fun open() {
        if (secured) secure()
        input.open()
        output.open()
        val deadline = now() + CONNECT_SECONDS
        while (input.streamStatus != NSStreamStatusOpen || output.streamStatus != NSStreamStatusOpen) {
            if (failed(input.streamStatus) || failed(output.streamStatus)) throw failure()
            if (now() > deadline) {
                close()
                throw MailNetworkException(tls = false)
            }
            usleep(POLL_MICROS)
        }
    }

    override fun line(): String {
        while (true) {
            val end = buffer.indexOf('\n'.code.toByte())
            if (end >= 0) {
                val line = buffer.copyOfRange(0, end).decodeToString().removeSuffix("\r")
                buffer = buffer.copyOfRange(end + 1, buffer.size)
                return line
            }
            val read = guarded { chunk.usePinned { input.read(it.addressOf(0).reinterpret<UByteVar>(), CHUNK.convert()) } }
            if (read <= 0) throw failure()
            buffer += chunk.copyOfRange(0, read.toInt())
        }
    }

    override fun write(text: String) {
        writeRaw(text.encodeToByteArray() + CRLF)
    }

    override fun writeRaw(bytes: ByteArray) {
        var sent = 0
        while (sent < bytes.size) {
            val n = guarded { bytes.usePinned { output.write(it.addressOf(sent).reinterpret<UByteVar>(), (bytes.size - sent).convert()) } }
            if (n <= 0) throw failure()
            sent += n.toInt()
        }
    }

    override fun upgrade() {
        secured = true
        secure()
    }

    override fun close() {
        input.close()
        output.close()
    }

    private fun secure() {
        input.setProperty(NSStreamSocketSecurityLevelNegotiatedSSL, forKey = NSStreamSocketSecurityLevelKey)
        output.setProperty(NSStreamSocketSecurityLevelNegotiatedSSL, forKey = NSStreamSocketSecurityLevelKey)
    }

    // Runs one blocking call; if it takes longer than the read timeout the streams are closed, which ends the call.
    private fun <T> guarded(call: () -> T): T {
        val ticket = watch.incrementAndGet()
        val queue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.convert(), 0u)
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, READ_NANOS), queue) {
            if (watch.value == ticket) close()
        }
        return try {
            call()
        } finally {
            watch.incrementAndGet()
        }
    }

    // TLS trouble shows as Secure Transport (-98xx) or URL-loading certificate (-120x) codes.
    private fun failure(): MailNetworkException {
        val code = (input.streamError ?: output.streamError)?.code ?: 0L
        return MailNetworkException(tls = code in -9899L..-9800L || code in -1206L..-1200L)
    }

    private fun failed(status: ULong) = status == NSStreamStatusError || status == NSStreamStatusAtEnd || status == NSStreamStatusClosed

    private fun now() = NSDate().timeIntervalSince1970
}

private const val CHUNK = 8192
private const val CONNECT_SECONDS = 10.0
private const val READ_NANOS = 30_000_000_000L
private const val POLL_MICROS = 10_000u
