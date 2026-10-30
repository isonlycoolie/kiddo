package app.kiddo.feature.onboarding.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.kiddo.core.designsystem.Kiddo
import app.kiddo.core.designsystem.component.Glow
import app.kiddo.core.designsystem.component.KiddoIcon
import app.kiddo.core.designsystem.component.KiddoScreen
import app.kiddo.core.designsystem.component.Mark
import app.kiddo.core.designsystem.component.Primary
import app.kiddo.core.designsystem.component.TextAction
import app.kiddo.core.designsystem.component.Wordmark
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.generated.KiddoGlyph
import app.kiddo.core.platform.platformFeel
import app.kiddo.core.resources.lock_body
import app.kiddo.core.resources.lock_body_apple
import app.kiddo.core.resources.lock_not_now
import app.kiddo.core.resources.lock_title
import app.kiddo.core.resources.lock_turn_on
import app.kiddo.core.resources.welcome_approve
import app.kiddo.core.resources.welcome_approve_body
import app.kiddo.core.resources.welcome_promise
import app.kiddo.core.resources.welcome_send
import app.kiddo.core.resources.welcome_send_body
import app.kiddo.core.resources.welcome_start
import app.kiddo.core.resources.welcome_tell
import app.kiddo.core.resources.welcome_tell_body
import app.kiddo.core.resources.welcome_title
import org.jetbrains.compose.resources.stringResource

// Splash: the face pops in over the glow, the word under it, once.
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(Kiddo.colors.bg), contentAlignment = Alignment.Center) {
        Glow(Modifier.offset(y = (-24).dp), size = 320.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Mark(size = 88.dp)
            Wordmark()
        }
    }
}

@Composable
fun WelcomeScreen(onStart: () -> Unit, modifier: Modifier = Modifier) {
    KiddoScreen(modifier, footer = { Primary(stringResource(Res.string.welcome_start), onStart) }) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Glow(Modifier.offset((-40).dp, 40.dp), size = 240.dp)
            Column(Modifier.align(Alignment.CenterStart).padding(horizontal = 24.dp)) {
                Mark(size = 52.dp)
                BasicText(
                    stringResource(Res.string.welcome_title),
                    style = Kiddo.type.hero.copy(color = Kiddo.colors.fg),
                    modifier = Modifier.padding(top = 22.dp),
                )
                Column(Modifier.padding(top = 30.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Promise(KiddoGlyph.NewChat, stringResource(Res.string.welcome_tell), stringResource(Res.string.welcome_tell_body))
                    Promise(KiddoGlyph.Mail, stringResource(Res.string.welcome_approve), stringResource(Res.string.welcome_approve_body))
                    Promise(KiddoGlyph.Sent, stringResource(Res.string.welcome_send), stringResource(Res.string.welcome_send_body))
                }
            }
        }
        BasicText(
            stringResource(Res.string.welcome_promise),
            style = Kiddo.type.sub.copy(color = Kiddo.colors.muted, textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun Promise(glyph: KiddoGlyph, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(40.dp).background(Kiddo.colors.well, CircleShape), contentAlignment = Alignment.Center) {
            KiddoIcon(glyph, null, size = 20.dp)
        }
        Column(Modifier.padding(top = 1.dp)) {
            BasicText(title, style = Kiddo.type.bodyStrong.copy(color = Kiddo.colors.fg))
            BasicText(body, style = Kiddo.type.sub.copy(color = Kiddo.colors.muted))
        }
    }
}

@Composable
fun LockSetupScreen(onTurnOn: () -> Unit, onSkip: () -> Unit, modifier: Modifier = Modifier) {
    KiddoScreen(
        modifier,
        footer = {
            Primary(stringResource(Res.string.lock_turn_on), onTurnOn, glyph = KiddoGlyph.Finger)
            TextAction(stringResource(Res.string.lock_not_now), onSkip)
        },
    ) {
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.size(80.dp).background(Kiddo.colors.well, CircleShape), contentAlignment = Alignment.Center) {
                KiddoIcon(KiddoGlyph.Finger, null, size = 38.dp, tint = Kiddo.colors.ok)
            }
            BasicText(
                stringResource(Res.string.lock_title),
                style = Kiddo.type.title.copy(color = Kiddo.colors.fg),
                modifier = Modifier.padding(top = 22.dp),
            )
            BasicText(
                stringResource(if (platformFeel.appleNames) Res.string.lock_body_apple else Res.string.lock_body),
                style = Kiddo.type.note.copy(color = Kiddo.colors.muted, textAlign = TextAlign.Center),
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}
