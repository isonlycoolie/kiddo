package app.kiddo.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.kiddo.core.model.CvData

// The Kiddo browser: a page the user can scroll and sign in to. `reader` is set once the page is up and returns what
// the user sees as plain text. Nothing is kept after it closes: cookies and storage are cleared.
@Composable
expect fun KiddoBrowser(url: String, onReader: (suspend () -> String) -> Unit, modifier: Modifier = Modifier)

// Draws a CV to a clean, one-column PDF with the phone's own PDF writer.
interface CvRenderer {
    suspend fun render(cv: CvData): ByteArray
}

// Local notifications: Kiddo has no server, so it never receives push; it tells the user what it found itself.
interface Notifier {
    fun show(id: Int, title: String, body: String)

    // A note for later (a follow-up's due time), replacing any earlier one with the same id; null `at` removes it.
    // Where Kiddo checks often in the background this does nothing.
    fun remind(id: Int, title: String, body: String, at: Long?) = Unit
}

// Asks once for permission to show notifications (Android 13 and up); a no-op where none is needed.
@Composable
expect fun rememberNotificationAsk(): () -> Unit

// Lets the user pick where a file goes (the system "save as"), then writes it. Calls back true when written.
@Composable
expect fun rememberFileSaver(onDone: (Boolean) -> Unit): (fileName: String, bytes: ByteArray) -> Unit

// Lets the user pick any file and reads it. Calls back null when cancelled or unreadable.
@Composable
expect fun rememberFileOpener(onFile: (ByteArray?) -> Unit): () -> Unit
