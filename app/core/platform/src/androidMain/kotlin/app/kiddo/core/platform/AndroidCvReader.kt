package app.kiddo.core.platform

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.ParcelFileDescriptor
import app.kiddo.core.model.CvFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipInputStream

// Reads CVs on the phone with the platform alone: PdfRenderer for pages (and text on Android 15+), the Word file's
// own XML for .docx. The PDF goes through a private cache file that is deleted straight after.
class AndroidCvReader(private val context: Context) : CvReader {
    override suspend fun read(bytes: ByteArray, format: CvFormat): CvReading? = withContext(Dispatchers.IO) {
        runCatching {
            when (format) {
                CvFormat.Pdf -> readPdf(bytes)
                CvFormat.Docx -> readDocx(bytes)
            }
        }.getOrNull()
    }

    private fun readPdf(bytes: ByteArray): CvReading = withPdf(context, bytes) { renderer ->
        val text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            (0 until renderer.pageCount).joinToString("\n\n") { index ->
                renderer.openPage(index).use { page -> page.textContents.joinToString("\n") { it.text } }
            }.trim().ifEmpty { null }
        } else {
            null
        }
        CvReading(renderer.pageCount, text)
    }

    // word/document.xml holds the text: paragraphs end at </w:p>, runs of text sit in <w:t>.
    private fun readDocx(bytes: ByteArray): CvReading? {
        ZipInputStream(bytes.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: return null
                if (entry.name != "word/document.xml") continue
                val xml = zip.readBytes().toString(Charsets.UTF_8)
                val text = xml.split("</w:p>").joinToString("\n") { paragraph ->
                    Regex("<w:t[^>]*>([^<]*)</w:t>").findAll(paragraph).joinToString("") { unescape(it.groupValues[1]) }
                }.lines().map(String::trim).filter(String::isNotEmpty).joinToString("\n")
                val pages = 1 + Regex("""w:type="page"""").findAll(xml).count()
                return CvReading(pages, text.ifEmpty { null })
            }
        }
    }

    private fun unescape(s: String) =
        s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&")
}

internal fun <T> withPdf(context: Context, bytes: ByteArray, block: (PdfRenderer) -> T): T {
    val file = File.createTempFile("cv", ".pdf", context.cacheDir)
    try {
        file.writeBytes(bytes)
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use(block)
        }
    } finally {
        file.delete()
    }
}
