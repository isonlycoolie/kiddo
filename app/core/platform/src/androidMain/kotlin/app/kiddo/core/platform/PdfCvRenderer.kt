package app.kiddo.core.platform

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import app.kiddo.core.model.CvData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

// The Clean template: A4, one column, ink on white, one hairline under the header, bold section titles, dates on the
// right, bullets that wrap. Pages break between lines, never inside one.
internal class PdfCvRenderer : CvRenderer {
    override suspend fun render(cv: CvData): ByteArray = withContext(Dispatchers.Default) {
        val doc = PdfDocument()
        val pen = Pen(doc)
        pen.text(cv.name, Style.Name)
        if (cv.headline.isNotBlank()) pen.text(cv.headline, Style.Headline)
        if (cv.contact.isNotEmpty()) pen.text(cv.contact.joinToString("  ·  "), Style.Muted)
        pen.rule()
        if (cv.summary.isNotBlank()) pen.text(cv.summary, Style.Body, gapAfter = 10f)
        cv.sections.forEach { section ->
            pen.gap(8f)
            pen.text(section.title.uppercase(), Style.Section, gapAfter = 6f)
            section.items.forEach { item ->
                if (item.title.isNotBlank() || item.dates.isNotBlank()) pen.titleWithDates(item.title, item.dates)
                if (item.subtitle.isNotBlank()) pen.text(item.subtitle, Style.Muted)
                item.bullets.forEach { pen.bullet(it) }
                pen.gap(6f)
            }
        }
        pen.finish()
        ByteArrayOutputStream().use { out ->
            doc.writeTo(out)
            doc.close()
            out.toByteArray()
        }
    }

    private enum class Style(val size: Float, val bold: Boolean, val grey: Boolean = false) {
        Name(22f, true),
        Headline(12f, false),
        Section(9.5f, true, grey = true),
        ItemTitle(10.5f, true),
        Body(10f, false),
        Muted(9.5f, false, grey = true),
    }

