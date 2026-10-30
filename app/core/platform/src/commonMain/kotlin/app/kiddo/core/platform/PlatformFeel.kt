package app.kiddo.core.platform

import androidx.compose.runtime.Composable

// Where iPhone and Android users expect different behaviour from the same screen. Features read these flags; they
// never ask which platform they run on.
class PlatformFeel(
    // iOS goes back with an edge swipe that drags the screen away under the finger; Android plays the designed Back.
    val backFollowsFinger: Boolean,
    // iOS chats put the keyboard away as you drag the messages down.
    val dragDismissesKeyboard: Boolean,
    // Android can keep Kiddo out of screenshots; iOS can only cover it in the app switcher.
    val blocksScreenshots: Boolean,
    // Apple's own words for the lock and the secure chip: Face ID, Touch ID, passcode, Secure Enclave.
    val appleNames: Boolean,
)

expect val platformFeel: PlatformFeel

// While on, the app switcher shows a cover instead of Kiddo (and screenshots are blocked where the platform allows).
@Composable
expect fun HideFromSwitcher(on: Boolean)
