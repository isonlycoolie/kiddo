package app.kiddo.core.platform

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

actual val platformFeel = PlatformFeel(
    backFollowsFinger = false,
    dragDismissesKeyboard = false,
    blocksScreenshots = true,
    appleNames = false,
)

// FLAG_SECURE blocks screenshots and blanks the app switcher preview.
@Composable
actual fun HideFromSwitcher(on: Boolean) {
    val window = (LocalContext.current as? Activity)?.window ?: return
    LaunchedEffect(on) {
        if (on) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE) else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}
