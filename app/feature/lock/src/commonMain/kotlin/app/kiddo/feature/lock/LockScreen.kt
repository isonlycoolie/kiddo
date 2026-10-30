package app.kiddo.feature.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.kiddo.core.designsystem.Kiddo
import app.kiddo.core.designsystem.component.Glow
import app.kiddo.core.designsystem.component.KiddoScreen
import app.kiddo.core.designsystem.component.Mark
import app.kiddo.core.designsystem.component.Primary
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.generated.KiddoGlyph
import app.kiddo.core.resources.lock_prompt
import app.kiddo.core.resources.locked
import app.kiddo.core.resources.unlock
import app.kiddo.core.security.UnlockResult
import app.kiddo.core.security.rememberDeviceUnlock
import org.jetbrains.compose.resources.stringResource

// Shown over everything while Kiddo is locked. It shows nothing of the workspace and asks once on arrival.
@Composable
fun LockScreen(onUnlocked: () -> Unit, modifier: Modifier = Modifier) {
    val ask = rememberDeviceUnlock(stringResource(Res.string.lock_prompt)) { result ->
        // A phone whose screen lock was removed can't lock Kiddo either, so it opens.
        if (result == UnlockResult.Unlocked || result == UnlockResult.NoDeviceLock) onUnlocked()
    }
    LaunchedEffect(Unit) { ask() }
    KiddoScreen(modifier, scrolls = false, footer = { Primary(stringResource(Res.string.unlock), ask, glyph = KiddoGlyph.Finger) }) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Glow(Modifier.offset(y = (-30).dp), size = 240.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Mark(size = 64.dp, soft = true)
                BasicText(stringResource(Res.string.locked), style = Kiddo.type.note.copy(color = Kiddo.colors.muted))
            }
        }
    }
}
