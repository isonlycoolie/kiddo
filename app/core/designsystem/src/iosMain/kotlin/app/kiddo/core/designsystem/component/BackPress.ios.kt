package app.kiddo.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler

// The iOS edge swipe is Back too: while a sheet or the menu is open it closes that instead of leaving the screen.
@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun BackPress(enabled: Boolean, onBack: () -> Unit) = BackHandler(enabled, onBack)
