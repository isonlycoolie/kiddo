package app.kiddo.core.platform

import app.kiddo.core.model.CvFormat
import app.kiddo.core.model.toNSData
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.withContext
import platform.PDFKit.PDFDocument
import platform.compression.COMPRESSION_ZLIB
import platform.compression.compression_decode_buffer
import kotlin.coroutines.CoroutineContext

// Reads CVs with the system alone: PDFKit for pages and text (on every iOS version), the Word file's own XML for
// .docx, unpacked from its zip with Apple's DEFLATE decoder.
class IosCvReader(private val io: CoroutineContext) : CvReader {
    override suspend fun read(bytes: ByteArray, format: CvFormat): CvReading? = withContext(io) {
        runCatching {
            when (format) {
                CvFormat.Pdf -> readPdf(bytes)
                CvFormat.Docx -> readDocx(bytes)
            }
        }.getOrNull()
    }

    private fun readPdf(bytes: ByteArray): CvReading {
        val doc = PDFDocument(data = bytes.toNSData())
        return CvReading(doc.pageCount.toInt(), doc.string?.trim()?.ifEmpty { null })
    }

    // word/document.xml holds the text: paragraphs end at </w:p>, runs of text sit in <w:t>.
    private fun readDocx(bytes: ByteArray): CvReading? {
        val xml = unzip(bytes, "word/document.xml")?.decodeToString() ?: return null
        val text = xml.split("</w:p>").joinToString("\n") { paragraph ->
            Regex("<w:t[^>]*>([^<]*)</w:t>").findAll(paragraph).joinToString("") { unescape(it.groupValues[1]) }
        }.lines().map(String::trim).filter(String::isNotEmpty).joinToString("\n")
        val pages = 1 + Regex("""w:type="page"""").findAll(xml).count()
        return CvReading(pages, text.ifEmpty { null })
    }

    private fun unescape(s: String) =
        s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&")
}

// One file out of a zip, found through the central directory. Stored and deflated entries only, as Word writes.
internal fun unzip(zip: ByteArray, name: String): ByteArray? {
    val end = (zip.size - END_SIZE downTo maxOf(0, zip.size - END_SIZE - MAX_COMMENT)).firstOrNull { u32(zip, it) == END_SIG } ?: return null
    var at = u32(zip, end + 16).toInt()
    repeat(u16(zip, end + 10)) {
        if (u32(zip, at) != ENTRY_SIG) return null
        val method = u16(zip, at + 10)
        val packed = u32(zip, at + 20).toInt()
        val size = u32(zip, at + 24).toInt()
        val nameLength = u16(zip, at + 28)
        val skip = nameLength + u16(zip, at + 30) + u16(zip, at + 32)
        val local = u32(zip, at + 42).toInt()
        if (zip.decodeToString(at + 46, at + 46 + nameLength) == name) {
            val start = local + 30 + u16(zip, local + 26) + u16(zip, local + 28)
            val data = zip.copyOfRange(start, start + packed)
            return when (method) {
                STORED -> data
                DEFLATED -> inflate(data, size)
                else -> null
            }
        }
        at += 46 + skip
    }
    return null
}

// COMPRESSION_ZLIB is raw DEFLATE (RFC 1951), exactly what zip entries hold.
@OptIn(ExperimentalForeignApi::class)
private fun inflate(data: ByteArray, size: Int): ByteArray? {
    if (size <= 0 || data.isEmpty()) return null
    val out = ByteArray(size)
    val written = out.usePinned { o ->
        data.usePinned { i ->
            compression_decode_buffer(
                o.addressOf(0).reinterpret<UByteVar>(),
                size.convert(),
                i.addressOf(0).reinterpret<UByteVar>(),
                data.size.convert(),
                null,
                COMPRESSION_ZLIB,
            )
        }
    }
    return if (written.toInt() == size) out else null
}

private fun u16(b: ByteArray, at: Int): Int = (b[at].toInt() and 0xFF) or ((b[at + 1].toInt() and 0xFF) shl 8)

private fun u32(b: ByteArray, at: Int): Long = u16(b, at).toLong() or (u16(b, at + 2).toLong() shl 16)

private const val END_SIG = 0x06054b50L
private const val ENTRY_SIG = 0x02014b50L
private const val END_SIZE = 22
private const val MAX_COMMENT = 65_535
private const val STORED = 0
private const val DEFLATED = 8
