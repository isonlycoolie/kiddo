package app.kiddo.core.platform

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kiddo.core.model.CvFormat
import app.kiddo.core.model.toByteArray
import app.kiddo.core.model.toNSData
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGSizeMake
import platform.PDFKit.PDFDocument
import platform.PDFKit.kPDFDisplayBoxMediaBox
import platform.UIKit.UIImagePNGRepresentation
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap

// A PDF page is drawn by PDFKit off the main thread at exactly the width it's shown; a Word CV shows its text on white.
@Composable
actual fun CvPage(bytes: ByteArray, format: CvFormat, text: String?, modifier: Modifier, page: Int) {
    BoxWithConstraints(modifier.background(Color.White)) {
        if (format == CvFormat.Docx) {
            BasicText(
                text.orEmpty(),
                style = TextStyle(color = Color(0xFF222222), fontSize = 7.sp, lineHeight = 9.sp),
                modifier = Modifier.padding(12.dp),
            )
            return@BoxWithConstraints
        }
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.roundToPx() }
        var image by remember(bytes, page, widthPx) { mutableStateOf<ImageBitmap?>(null) }
        LaunchedEffect(bytes, page, widthPx) {
            image = withContext(Dispatchers.Default) { runCatching { render(bytes, page, widthPx, density.density.toDouble()) }.getOrNull() }
        }
        image?.let { Image(it, null, Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth) }
            ?: Box(Modifier.fillMaxWidth())
    }
}

@OptIn(ExperimentalForeignApi::class, ExperimentalResourceApi::class)
private fun render(bytes: ByteArray, index: Int, widthPx: Int, scale: Double): ImageBitmap? {
    if (widthPx <= 0) return null
    val doc = PDFDocument(data = bytes.toNSData())
    if (index !in 0 until doc.pageCount.toInt()) return null
    val page = doc.pageAtIndex(index.toULong()) ?: return null
    val (w, h) = page.boundsForBox(kPDFDisplayBoxMediaBox).useContents { size.width to size.height }
    if (w <= 0.0) return null
    // Thumbnail sizes are in points; the screen scale turns them into exactly widthPx pixels.
    val points = widthPx / scale
    val picture = page.thumbnailOfSize(CGSizeMake(points, points * h / w), forBox = kPDFDisplayBoxMediaBox)
    val png = UIImagePNGRepresentation(picture) ?: return null
    return png.toByteArray().decodeToImageBitmap()
}
