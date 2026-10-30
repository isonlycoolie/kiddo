package app.kiddo.feature.email

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.kiddo.core.designsystem.Kiddo
import app.kiddo.core.designsystem.component.BrandLogoImage
import app.kiddo.core.designsystem.component.Field
import app.kiddo.core.designsystem.component.FieldKind
import app.kiddo.core.designsystem.component.Group
import app.kiddo.core.designsystem.component.KiddoScreen
import app.kiddo.core.designsystem.component.ListRow
import app.kiddo.core.designsystem.component.NavBar
import app.kiddo.core.designsystem.component.Note
import app.kiddo.core.designsystem.component.PickerField
import app.kiddo.core.designsystem.component.Primary
import app.kiddo.core.designsystem.component.Title
import app.kiddo.core.designsystem.component.message
import app.kiddo.core.designsystem.component.sub
import app.kiddo.core.model.MailSecurity
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.generated.BrandLogo
import app.kiddo.core.resources.generated.KiddoGlyph
import app.kiddo.core.resources.gmail_address
import app.kiddo.core.resources.gmail_app_password
import app.kiddo.core.resources.gmail_app_password_note
import app.kiddo.core.resources.gmail_password_title
import app.kiddo.core.resources.mail_stored
import app.kiddo.core.resources.mail_test_connect
import app.kiddo.core.resources.security_ssl
import app.kiddo.core.resources.security_starttls
import app.kiddo.core.resources.sendly_connect
import app.kiddo.core.resources.sendly_from
import app.kiddo.core.resources.sendly_from_note
import app.kiddo.core.resources.sendly_key
import app.kiddo.core.resources.sendly_no_attach
import app.kiddo.core.resources.sendly_open
import app.kiddo.core.resources.sendly_open_sub
import app.kiddo.core.resources.sendly_sub
import app.kiddo.core.resources.sendly_title
import app.kiddo.core.resources.smtp_address
import app.kiddo.core.resources.smtp_encrypted
import app.kiddo.core.resources.smtp_imap_note
import app.kiddo.core.resources.smtp_password
import app.kiddo.core.resources.smtp_port
import app.kiddo.core.resources.smtp_reading
import app.kiddo.core.resources.smtp_security
import app.kiddo.core.resources.smtp_sending
import app.kiddo.core.resources.smtp_server
import app.kiddo.core.resources.smtp_title
import app.kiddo.core.resources.smtp_username
import org.jetbrains.compose.resources.stringResource

class FormActions(val onBack: () -> Unit, val onEdit: ((ConnectForm) -> ConnectForm) -> Unit, val onSubmit: () -> Unit)

@Composable
private fun failureText(form: ConnectForm): String? = form.failure?.let { stringResource(it.message()) }

@Composable
fun GmailPasswordScreen(form: ConnectForm, actions: FormActions, modifier: Modifier = Modifier) {
    val ready = form.address.contains('@') && form.secret.replace(" ", "").length >= APP_PASSWORD
    KiddoScreen(
        modifier,
        header = { NavBar(onBack = actions.onBack) },
        footer = { Primary(stringResource(Res.string.mail_test_connect), actions.onSubmit, enabled = ready, busy = form.testing) },
    ) {
        Title(stringResource(Res.string.gmail_password_title))
        Field(form.address, { v -> actions.onEdit { it.copy(address = v) } }, label = stringResource(Res.string.gmail_address), kind = FieldKind.Email)
        Field(
            form.secret,
            { v -> actions.onEdit { it.copy(secret = v) } },
            label = stringResource(Res.string.gmail_app_password),
            kind = FieldKind.Secret,
            note = stringResource(Res.string.gmail_app_password_note),
            error = failureText(form),
        )
        Note(stringResource(Res.string.mail_stored))
    }
}

