package app.kiddo.core.platform

import app.kiddo.core.model.CvData
import app.kiddo.core.model.toByteArray
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSString
import platform.UIKit.NSFontAttributeName
import platform.UIKit.NSForegroundColorAttributeName
import platform.UIKit.UIBezierPath
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UIGraphicsPDFRenderer
import platform.UIKit.UIGraphicsPDFRendererContext
import platform.UIKit.UIGraphicsPDFRendererFormat
import platform.UIKit.drawAtPoint
import platform.UIKit.sizeWithAttributes

// The Clean template, drawn with UIKit's PDF renderer exactly as Android draws it: A4, one column, ink on white, one
// hairline under the header, bold section titles, dates on the right, bullets that wrap. Pages break between lines.
@OptIn(ExperimentalForeignApi::class)
class IosCvRenderer : CvRenderer {
    override suspend fun render(cv: CvData): ByteArray = withContext(Dispatchers.Default) {
        val renderer = UIGraphicsPDFRenderer(bounds = CGRectMake(0.0, 0.0, WIDTH, HEIGHT), format = UIGraphicsPDFRendererFormat())
        renderer.PDFDataWithActions { context ->
            val pen = Pen(context ?: return@PDFDataWithActions)
            pen.text(cv.name, Style.Name)
            if (cv.headline.isNotBlank()) pen.text(cv.headline, Style.Headline)
            if (cv.contact.isNotEmpty()) pen.text(cv.contact.joinToString("  ·  "), Style.Muted)
            pen.rule()
            if (cv.summary.isNotBlank()) pen.text(cv.summary, Style.Body, gapAfter = 10.0)
            cv.sections.forEach { section ->
                pen.gap(8.0)
                pen.text(section.title.uppercase(), Style.Section, gapAfter = 6.0)
                section.items.forEach { item ->
                    if (item.title.isNotBlank() || item.dates.isNotBlank()) pen.titleWithDates(item.title, item.dates)
                    if (item.subtitle.isNotBlank()) pen.text(item.subtitle, Style.Muted)
                    item.bullets.forEach { pen.bullet(it) }
                    pen.gap(6.0)
                }
            }
        }.toByteArray()
    }

    private enum class Style(val size: Double, val bold: Boolean, val grey: Boolean = false) {
        Name(22.0, true),
        Headline(12.0, false),
        Section(9.5, true, grey = true),
        ItemTitle(10.5, true),
        Body(10.0, false),
        Muted(9.5, false, grey = true),
    }

    private class Pen(private val context: UIGraphicsPDFRendererContext) {
        private var y = MARGIN

        init {
            context.beginPage()
        }

        private fun font(style: Style) = if (style.bold) UIFont.boldSystemFontOfSize(style.size) else UIFont.systemFontOfSize(style.size)

        private fun attrs(style: Style): Map<Any?, Any?> =
            mapOf(NSFontAttributeName to font(style), NSForegroundColorAttributeName to if (style.grey) GREY else INK)

        @Suppress("CAST_NEVER_SUCCEEDS")
        private fun width(text: String, style: Style): Double = (text as NSString).sizeWithAttributes(attrs(style)).useContents { width }

        // Text is placed by its baseline, as on Android; UIKit draws from the top of the line box.
        @Suppress("CAST_NEVER_SUCCEEDS")
        private fun draw(text: String, style: Style, x: Double, baseline: Double) {
            (text as NSString).drawAtPoint(CGPointMake(x, baseline - font(style).ascender), withAttributes = attrs(style))
        }

        private fun room(height: Double) {
            if (y + height <= HEIGHT - MARGIN) return
            context.beginPage()
            y = MARGIN
        }

        fun gap(points: Double) {
            y += points
        }

        fun text(text: String, style: Style, gapAfter: Double = 3.0, indent: Double = 0.0) {
            wrap(text, style, CONTENT - indent).forEach { line ->
                room(style.size * LEADING)
                y += style.size * LEADING
                draw(line, style, MARGIN + indent, y - style.size * 0.3)
            }
            y += gapAfter
        }

        fun titleWithDates(title: String, dates: String) {
            room(Style.ItemTitle.size * LEADING)
            y += Style.ItemTitle.size * LEADING
            val base = y - Style.ItemTitle.size * 0.3
            val datesWidth = width(dates, Style.Muted)
            draw(ellipsize(title, Style.ItemTitle, CONTENT - datesWidth - 12.0), Style.ItemTitle, MARGIN, base)
            draw(dates, Style.Muted, WIDTH - MARGIN - datesWidth, base)
            y += 2.0
        }

        fun bullet(text: String) {
            room(Style.Body.size * LEADING)
            val cy = y + Style.Body.size * LEADING - Style.Body.size * 0.62
            INK.setFill()
            UIBezierPath.bezierPathWithOvalInRect(CGRectMake(MARGIN + 3.0 - 1.4, cy - 1.4, 2.8, 2.8)).fill()
            text(text, Style.Body, gapAfter = 1.5, indent = 12.0)
        }

        fun rule() {
            y += 6.0
            val line = UIBezierPath()
            line.moveToPoint(CGPointMake(MARGIN, y))
            line.addLineToPoint(CGPointMake(WIDTH - MARGIN, y))
            line.lineWidth = 0.6
            RULE.setStroke()
            line.stroke()
            y += 12.0
        }

        private fun wrap(text: String, style: Style, limit: Double): List<String> {
            val lines = mutableListOf<String>()
            text.split('\n').forEach { para ->
                var line = ""
                para.split(' ').filter { it.isNotEmpty() }.forEach { word ->
                    val tryLine = if (line.isEmpty()) word else "$line $word"
                    if (width(tryLine, style) <= limit) {
                        line = tryLine
                    } else {
                        if (line.isNotEmpty()) lines += line
                        line = word
                    }
                }
                lines += line
            }
            return lines
        }

        private fun ellipsize(text: String, style: Style, limit: Double): String {
            if (width(text, style) <= limit) return text
            var cut = text
            while (cut.isNotEmpty() && width("$cut…", style) > limit) cut = cut.dropLast(1)
            return "$cut…"
        }
    }

    private companion object {
        const val WIDTH = 595.0
        const val HEIGHT = 842.0
        const val MARGIN = 48.0
        const val CONTENT = WIDTH - 2 * MARGIN
        const val LEADING = 1.38
        val INK: UIColor = UIColor.colorWithRed(17 / 255.0, 17 / 255.0, 17 / 255.0, 1.0)
        val GREY: UIColor = UIColor.colorWithRed(110 / 255.0, 110 / 255.0, 110 / 255.0, 1.0)
        val RULE: UIColor = UIColor.colorWithRed(220 / 255.0, 220 / 255.0, 220 / 255.0, 1.0)
    }
}
