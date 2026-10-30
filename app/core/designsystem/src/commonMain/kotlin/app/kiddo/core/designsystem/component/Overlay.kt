package app.kiddo.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.kiddo.core.designsystem.Kiddo
import app.kiddo.core.designsystem.KiddoMotion
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.action_close
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

// Any screen can raise a sheet or a toast; the host draws them over everything. Sheets pick or confirm and close.
@Stable
class Overlay {
    internal var content by mutableStateOf<(@Composable ColumnScope.() -> Unit)?>(null)
    internal var title by mutableStateOf<String?>(null)
    internal var open by mutableStateOf(false)
    internal var toast by mutableStateOf<String?>(null)
    internal var toastKey by mutableIntStateOf(0)

    fun sheet(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
        this.title = title
        this.content = content
        open = true
    }

    fun close() {
        open = false
    }

    fun toast(text: String) {
        close()
        toast = text
        toastKey++
    }
}

val LocalOverlay = staticCompositionLocalOf<Overlay> { error("OverlayHost is missing") }

