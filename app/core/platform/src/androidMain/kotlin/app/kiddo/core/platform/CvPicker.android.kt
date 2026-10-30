package app.kiddo.core.platform

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import app.kiddo.core.model.CvFormat

// The system document picker, limited to PDF and Word. Nothing is read beyond the one file the user chose.
@Composable
actual fun rememberCvPicker(onResult: (PickResult) -> Unit): () -> Unit {
    val context = LocalContext.current
    val latest by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val resolver = context.contentResolver
        val format = CvFormat.entries.firstOrNull { it.mimeType == resolver.getType(uri) }
        if (format == null) {
            latest(PickResult.Unsupported)
            return@rememberLauncherForActivityResult
        }
        var name = "CV"
        var size = 0L
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use {
            if (it.moveToFirst()) {
                name = it.getString(0) ?: name
                size = it.getLong(1)
            }
        }
        if (size > MAX_CV_BYTES) {
            latest(PickResult.TooLarge)
            return@rememberLauncherForActivityResult
        }
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
        latest(
            when {
                bytes == null -> PickResult.Unsupported
                bytes.size > MAX_CV_BYTES -> PickResult.TooLarge
                else -> PickResult.Picked(PickedFile(name.substringBeforeLast('.'), format, bytes))
            },
        )
    }
    return remember(launcher) { { launcher.launch(CvFormat.entries.map { it.mimeType }.toTypedArray()) } }
}
