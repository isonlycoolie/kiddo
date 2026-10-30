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

