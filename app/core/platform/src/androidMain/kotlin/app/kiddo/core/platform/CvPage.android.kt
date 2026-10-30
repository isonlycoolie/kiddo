package app.kiddo.core.platform

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.provider.Settings
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kiddo.core.model.CvFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.Color as ComposeColor

// A PDF page is rendered off the main thread at exactly the width it's shown; a Word CV shows its text on white.
@Composable
actual fun CvPage(bytes: ByteArray, format: CvFormat, text: String?, modifier: Modifier, page: Int) {
    BoxWithConstraints(modifier.background(ComposeColor.White)) {
        if (format == CvFormat.Docx) {
            BasicText(
                text.orEmpty(),
                style = TextStyle(color = ComposeColor(0xFF222222), fontSize = 7.sp, lineHeight = 9.sp),
                modifier = Modifier.padding(12.dp),
            )
            return@BoxWithConstraints
        }
        val context = LocalContext.current
        val widthPx = with(LocalDensity.current) { maxWidth.roundToPx() }
        var image by remember(bytes, page, widthPx) { mutableStateOf<ImageBitmap?>(null) }
        LaunchedEffect(bytes, page, widthPx) {
            image = withContext(Dispatchers.IO) {
                runCatching { withPdf(context, bytes) { render(it, page, widthPx) } }.getOrNull()
            }
        }
        image?.let { Image(it, null, Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth) }
            ?: Box(Modifier.fillMaxWidth())
    }
}

private fun render(renderer: PdfRenderer, index: Int, widthPx: Int): ImageBitmap? {
    if (index !in 0 until renderer.pageCount || widthPx <= 0) return null
    return renderer.openPage(index).use { page ->
        val height = widthPx * page.height / page.width
        val bitmap = Bitmap.createBitmap(widthPx, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        bitmap.asImageBitmap()
    }
}

// Android's "Remove animations" sets the animator duration scale to 0.
@Composable
actual fun rememberReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}
