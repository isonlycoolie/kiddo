package app.kiddo.core.model

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.posix.memcpy

// Kotlin bytes to Foundation data and back, copied once each way.
@OptIn(ExperimentalForeignApi::class)
fun ByteArray.toNSData(): NSData = usePinned { pinned ->
    NSData.create(bytes = if (isEmpty()) null else pinned.addressOf(0), length = size.toULong())
}

@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray(): ByteArray {
    val out = ByteArray(length.toInt())
    if (out.isNotEmpty()) out.usePinned { memcpy(it.addressOf(0), bytes, length) }
    return out
}
