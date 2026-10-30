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

    private class Pen(private val doc: PdfDocument) {
        private var number = 0
        private var page: PdfDocument.Page = next()
        private var y = MARGIN

        private fun next(): PdfDocument.Page {
            number++
            return doc.startPage(PdfDocument.PageInfo.Builder(WIDTH, HEIGHT, number).create())
        }

        private val canvas: Canvas get() = page.canvas

        private fun paint(style: Style) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = style.size
            typeface = Typeface.create(Typeface.SANS_SERIF, if (style.bold) Typeface.BOLD else Typeface.NORMAL)
            color = if (style.grey) GREY else INK
        }

        private fun room(height: Float) {
            if (y + height <= HEIGHT - MARGIN) return
            doc.finishPage(page)
            page = next()
            y = MARGIN
        }

        fun gap(points: Float) {
            y += points
        }

        fun text(text: String, style: Style, gapAfter: Float = 3f, indent: Float = 0f) {
            val p = paint(style)
            wrap(text, p, CONTENT - indent).forEach { line ->
                room(style.size * LEADING)
                y += style.size * LEADING
                canvas.drawText(line, MARGIN + indent, y - style.size * 0.3f, p)
            }
            y += gapAfter
        }

        fun titleWithDates(title: String, dates: String) {
            val p = paint(Style.ItemTitle)
            val d = paint(Style.Muted)
            room(Style.ItemTitle.size * LEADING)
            y += Style.ItemTitle.size * LEADING
            val base = y - Style.ItemTitle.size * 0.3f
            canvas.drawText(ellipsize(title, p, CONTENT - d.measureText(dates) - 12f), MARGIN, base, p)
            canvas.drawText(dates, WIDTH - MARGIN - d.measureText(dates), base, d)
            y += 2f
        }

        fun bullet(text: String) {
            val p = paint(Style.Body)
            room(Style.Body.size * LEADING)
            canvas.drawCircle(MARGIN + 3f, y + Style.Body.size * LEADING - Style.Body.size * 0.62f, 1.4f, p)
            text(text, Style.Body, gapAfter = 1.5f, indent = 12f)
        }

        fun rule() {
            y += 6f
            canvas.drawLine(MARGIN, y, WIDTH - MARGIN, y, Paint().apply { color = RULE; strokeWidth = 0.6f })
            y += 12f
        }

        fun finish() = doc.finishPage(page)

        private fun wrap(text: String, p: Paint, width: Float): List<String> {
            val lines = mutableListOf<String>()
            text.split('\n').forEach { para ->
                var line = ""
                para.split(' ').filter { it.isNotEmpty() }.forEach { word ->
                    val tryLine = if (line.isEmpty()) word else "$line $word"
                    if (p.measureText(tryLine) <= width) line = tryLine else {
                        if (line.isNotEmpty()) lines += line
                        line = word
                    }
                }
                lines += line
            }
            return lines
        }

        private fun ellipsize(text: String, p: Paint, width: Float): String {
            if (p.measureText(text) <= width) return text
            var cut = text
            while (cut.isNotEmpty() && p.measureText("$cut…") > width) cut = cut.dropLast(1)
            return "$cut…"
        }
    }

    private companion object {
        const val WIDTH = 595
        const val HEIGHT = 842
        const val MARGIN = 48f
        const val CONTENT = WIDTH - 2 * MARGIN
        const val LEADING = 1.38f
        val INK = Color.rgb(17, 17, 17)
        val GREY = Color.rgb(110, 110, 110)
        val RULE = Color.rgb(220, 220, 220)
    }
}
