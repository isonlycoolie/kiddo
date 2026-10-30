package app.kiddo.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import kotlinx.coroutines.flow.MutableSharedFlow

// Asks the page on screen to scroll back to its top, as a tap on the iPhone status bar does (the iOS shell reports
// the tap). Screens with their own list scroll it themselves through ScrollToTopOnRequest.
object ScrollToTop {
    internal val requests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun request() {
        requests.tryEmit(Unit)
    }
}

@Composable
fun ScrollToTopOnRequest(scroll: suspend () -> Unit) {
    val latest = rememberUpdatedState(scroll)
    LaunchedEffect(Unit) { ScrollToTop.requests.collect { latest.value() } }
}

// For a chat's message list: dragging the messages down puts the keyboard away, as iPhone chats do. Off where the
// platform keeps the keyboard up.
@Composable
fun rememberKeyboardAwayOnDrag(enabled: Boolean): NestedScrollConnection {
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    return remember(enabled, keyboard, focus) { KeyboardAwayOnDrag(enabled, keyboard, focus) }
}

private class KeyboardAwayOnDrag(
    private val enabled: Boolean,
    private val keyboard: SoftwareKeyboardController?,
    private val focus: FocusManager,
) : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (enabled && available.y > 0f && source == NestedScrollSource.UserInput) {
            keyboard?.hide()
            focus.clearFocus()
        }
        return Offset.Zero
    }
}
