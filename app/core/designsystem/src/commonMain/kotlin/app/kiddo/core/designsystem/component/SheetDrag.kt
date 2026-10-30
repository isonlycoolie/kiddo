package app.kiddo.core.designsystem.component

import androidx.compose.animation.core.animate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import app.kiddo.core.designsystem.KiddoMotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// Past a third of its height, or flicked down faster than this, a dragged sheet closes; otherwise it settles back.
private const val DISMISS_FRACTION = 0.33f
private val DismissSpeed = 900.dp

// Drag a sheet down by its grabber or its content (once the content is at its top) and it follows the finger, as
// system sheets do; the content's own scrolling always comes first.
@Composable
internal fun rememberSheetDrag(onDismiss: () -> Unit): SheetDrag {
    val scope = rememberCoroutineScope()
    val dismiss = rememberUpdatedState(onDismiss)
    val speed = with(LocalDensity.current) { DismissSpeed.toPx() }
    return remember(scope, speed) { SheetDrag(scope, speed) { dismiss.value() } }
}

internal class SheetDrag(
    private val scope: CoroutineScope,
    private val dismissSpeed: Float,
    private val onDismiss: () -> Unit,
) : NestedScrollConnection {
    var offset by mutableFloatStateOf(0f)
        private set
    var height = 0f

    fun reset() {
        offset = 0f
    }

    // Dragging back up first gives back the distance the sheet was pulled down.
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (offset <= 0f || available.y >= 0f || source != NestedScrollSource.UserInput) return Offset.Zero
        val used = maxOf(available.y, -offset)
        offset += used
        return Offset(0f, used)
    }

    // What the content could not scroll (it is at its top) pulls the sheet down.
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (available.y <= 0f || source != NestedScrollSource.UserInput) return Offset.Zero
        offset += available.y
        return Offset(0f, available.y)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (offset <= 0f) return Velocity.Zero
        settle(available.y)
        return available
    }

    private fun settle(velocity: Float) {
        if (velocity > dismissSpeed || offset > height * DISMISS_FRACTION) {
            onDismiss()
            return
        }
        val back = KiddoMotion.tween<Float>(KiddoMotion.QUICK, KiddoMotion.Sheet)
        scope.launch { animate(offset, 0f, animationSpec = back) { value, _ -> offset = value } }
    }
}
