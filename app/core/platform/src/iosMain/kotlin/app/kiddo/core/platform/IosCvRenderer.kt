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

