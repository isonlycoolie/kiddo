package app.kiddo.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.kiddo.core.model.CvFormat

// A file the user picked, read into memory. CVs are small; anything over the limit is refused before reading.
class PickedFile(val name: String, val format: CvFormat, val bytes: ByteArray)

sealed interface PickResult {
    data class Picked(val file: PickedFile) : PickResult

    data object TooLarge : PickResult

    data object Unsupported : PickResult
}

// Opens the system file picker for a PDF or Word CV. Returns the function that shows it.
@Composable
expect fun rememberCvPicker(onResult: (PickResult) -> Unit): () -> Unit

// What Kiddo could read from a CV on the phone. `text` is null when this phone can't read the PDF's text.
data class CvReading(val pages: Int, val text: String?)

interface CvReader {
    suspend fun read(bytes: ByteArray, format: CvFormat): CvReading?
}

// One CV page drawn at the size it's shown (PDF), or the read text on a white page (Word).
@Composable
expect fun CvPage(bytes: ByteArray, format: CvFormat, text: String?, modifier: Modifier = Modifier, page: Int = 0)

@Composable
expect fun rememberReducedMotion(): Boolean

const val MAX_CV_BYTES = 10 * 1024 * 1024

// The local calendar day a moment falls on (days since 1970 in the phone's time zone), for "Today" and "Yesterday".
expect fun localDay(epochMillis: Long): Long

// "24 Oct" in the phone's language and time zone.
expect fun shortDate(epochMillis: Long): String

// Saturday or Sunday where the phone is, so follow-ups wait for a working day.
expect fun isWeekend(epochMillis: Long): Boolean
