package app.kiddo.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.kiddo.core.designsystem.Kiddo
import app.kiddo.core.designsystem.component.Glow
import app.kiddo.core.designsystem.component.Group
import app.kiddo.core.designsystem.component.Hairline
import app.kiddo.core.designsystem.component.KiddoScreen
import app.kiddo.core.designsystem.component.ListRow
import app.kiddo.core.designsystem.component.Mark
import app.kiddo.core.designsystem.component.NavBar
import app.kiddo.core.designsystem.component.Tick
import app.kiddo.core.designsystem.component.Toggle
import app.kiddo.core.designsystem.component.Wordmark
import app.kiddo.core.designsystem.component.pressable
import app.kiddo.core.designsystem.component.sub
import app.kiddo.core.model.AppInfo
import app.kiddo.core.model.Appearance
import app.kiddo.core.model.LockAfter
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.about_body
import app.kiddo.core.resources.about_licence
import app.kiddo.core.resources.about_made_by
import app.kiddo.core.resources.about_source
import app.kiddo.core.resources.about_version
import app.kiddo.core.resources.appearance_dark
import app.kiddo.core.resources.appearance_light
import app.kiddo.core.resources.appearance_system
import app.kiddo.core.resources.generated.KiddoGlyph
import app.kiddo.core.resources.lock_after_five
import app.kiddo.core.resources.lock_after_one
import app.kiddo.core.resources.lock_after_right_away
import app.kiddo.core.resources.security_encrypted
import app.kiddo.core.resources.security_encrypted_sub
import app.kiddo.core.resources.security_hide
import app.kiddo.core.platform.platformFeel
import app.kiddo.core.resources.security_enclave
import app.kiddo.core.resources.security_hide_note
import app.kiddo.core.resources.security_hide_note_apple
import app.kiddo.core.resources.security_keychain
import app.kiddo.core.resources.security_keys
import app.kiddo.core.resources.security_lock
import app.kiddo.core.resources.security_lock_after
import app.kiddo.core.resources.security_sealed
import app.kiddo.core.resources.security_strongbox
import app.kiddo.core.resources.security_tee
import app.kiddo.core.resources.settings_about
import app.kiddo.core.resources.settings_ai
import app.kiddo.core.resources.settings_backup
import app.kiddo.core.resources.settings_followups
import app.kiddo.core.resources.settings_appearance
import app.kiddo.core.resources.settings_email
import app.kiddo.core.resources.settings_erase
import app.kiddo.core.resources.settings_lock_off
import app.kiddo.core.resources.settings_lock_on
import app.kiddo.core.resources.settings_none
import app.kiddo.core.resources.settings_security
import app.kiddo.core.resources.settings_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

class SettingsActions(
    val onBack: () -> Unit,
    val onEmail: () -> Unit,
    val onAi: () -> Unit,
    val onFollowUps: () -> Unit,
    val onSecurity: () -> Unit,
    val onBackup: () -> Unit,
    val onAppearance: () -> Unit,
    val onAbout: () -> Unit,
    val onErase: () -> Unit,
)

@Composable
fun SettingsScreen(state: SettingsState, actions: SettingsActions, modifier: Modifier = Modifier) {
    KiddoScreen(modifier, header = { NavBar(title = stringResource(Res.string.settings_title), onBack = actions.onBack) }) {
        Group {
            ListRow(stringResource(Res.string.settings_email), glyph = KiddoGlyph.Mail, value = state.mail?.address ?: stringResource(Res.string.settings_none), onClick = actions.onEmail)
            Hairline()
            ListRow(stringResource(Res.string.settings_ai), glyph = KiddoGlyph.Ai, value = state.providers.firstOrNull()?.provider?.label ?: stringResource(Res.string.settings_none), onClick = actions.onAi)
            Hairline()
            ListRow(stringResource(Res.string.settings_followups), glyph = KiddoGlyph.Refresh, onClick = actions.onFollowUps)
        }
        Group {
            val lock = if (state.settings.lockEnabled) Res.string.settings_lock_on else Res.string.settings_lock_off
            ListRow(stringResource(Res.string.settings_security), glyph = KiddoGlyph.Shield, value = stringResource(lock), onClick = actions.onSecurity)
            Hairline()
            ListRow(stringResource(Res.string.settings_backup), glyph = KiddoGlyph.Backup, onClick = actions.onBackup)
        }
        Group {
            ListRow(stringResource(Res.string.settings_appearance), glyph = KiddoGlyph.Moon, value = stringResource(appearanceLabel(state.settings.appearance)), onClick = actions.onAppearance)
            Hairline()
            ListRow(stringResource(Res.string.settings_about), glyph = KiddoGlyph.Info, onClick = actions.onAbout)
        }
        Group { ListRow(stringResource(Res.string.settings_erase), glyph = KiddoGlyph.Trash, danger = true, onClick = actions.onErase) }
    }
}

fun appearanceLabel(a: Appearance): StringResource = when (a) {
    Appearance.System -> Res.string.appearance_system
    Appearance.Light -> Res.string.appearance_light
    Appearance.Dark -> Res.string.appearance_dark
}

fun lockAfterLabel(a: LockAfter): StringResource = when (a) {
    LockAfter.RightAway -> Res.string.lock_after_right_away
    LockAfter.OneMinute -> Res.string.lock_after_one
    LockAfter.FiveMinutes -> Res.string.lock_after_five
}

class SecurityActions(
    val onBack: () -> Unit,
    val onLock: (Boolean) -> Unit,
    val onLockAfter: () -> Unit,
    val onHide: (Boolean) -> Unit,
    val onErase: () -> Unit,
)

@Composable
fun SecurityScreen(state: SettingsState, actions: SecurityActions, modifier: Modifier = Modifier) {
    KiddoScreen(modifier, header = { NavBar(title = stringResource(Res.string.settings_security), onBack = actions.onBack) }) {
        val s = state.settings
        val lockLabel = stringResource(Res.string.security_lock)
        Group {
            ListRow(lockLabel, glyph = KiddoGlyph.Finger, trailing = { Toggle(s.lockEnabled, actions.onLock, lockLabel) })
            if (s.lockEnabled) {
                Hairline()
                ListRow(stringResource(Res.string.security_lock_after), value = stringResource(lockAfterLabel(s.lockAfter)), onClick = actions.onLockAfter)
            }
        }
        val hideLabel = stringResource(Res.string.security_hide)
        Group(note = stringResource(if (platformFeel.blocksScreenshots) Res.string.security_hide_note else Res.string.security_hide_note_apple)) {
            ListRow(hideLabel, trailing = { Toggle(s.hideInSwitcher, actions.onHide, hideLabel) })
        }
        Group(title = stringResource(Res.string.security_keys)) {
            val chip = when {
                platformFeel.appleNames -> if (state.hardware.strongBox) Res.string.security_enclave else Res.string.security_keychain
                state.hardware.strongBox -> Res.string.security_strongbox
                else -> Res.string.security_tee
            }
            ListRow(stringResource(Res.string.security_sealed), glyph = KiddoGlyph.Key, sub = sub(stringResource(chip)), trailing = { Tick() })
            Hairline()
            ListRow(stringResource(Res.string.security_encrypted), glyph = KiddoGlyph.Lock, sub = sub(stringResource(Res.string.security_encrypted_sub)), trailing = { Tick() })
        }
        Group { ListRow(stringResource(Res.string.settings_erase), glyph = KiddoGlyph.Trash, danger = true, onClick = actions.onErase) }
    }
}

