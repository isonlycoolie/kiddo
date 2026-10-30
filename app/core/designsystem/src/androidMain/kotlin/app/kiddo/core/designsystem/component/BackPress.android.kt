package app.kiddo.core.designsystem.component

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun BackPress(enabled: Boolean, onBack: () -> Unit) = BackHandler(enabled, onBack)
