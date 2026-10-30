package app.kiddo.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled
import platform.UIKit.UIAccessibilityReduceMotionStatusDidChangeNotification

actual val platformFeel = PlatformFeel(
    backFollowsFinger = true,
    dragDismissesKeyboard = true,
    blocksScreenshots = false,
    appleNames = true,
)

// Read by the iOS shell when Kiddo leaves the front: while on, it lays a plain cover over the app switcher snapshot.
object SwitcherCover {
    var on: Boolean = true
        internal set
}

@Composable
actual fun HideFromSwitcher(on: Boolean) {
    LaunchedEffect(on) { SwitcherCover.on = on }
}

// Settings > Accessibility > Motion > Reduce Motion, followed live as iOS apps do.
@Composable
actual fun rememberReducedMotion(): Boolean {
    var reduced by remember { mutableStateOf(UIAccessibilityIsReduceMotionEnabled()) }
    DisposableEffect(Unit) {
        val center = NSNotificationCenter.defaultCenter
        val observer = center.addObserverForName(UIAccessibilityReduceMotionStatusDidChangeNotification, null, NSOperationQueue.mainQueue) { _ ->
            reduced = UIAccessibilityIsReduceMotionEnabled()
        }
        onDispose { center.removeObserver(observer) }
    }
    return reduced
}
