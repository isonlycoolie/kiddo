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

