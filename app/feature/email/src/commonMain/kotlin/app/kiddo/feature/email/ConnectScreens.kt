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

@Composable
fun SmtpScreen(form: ConnectForm, onPickSecurity: () -> Unit, actions: FormActions, modifier: Modifier = Modifier) {
    val ready = form.address.contains('@') && form.smtpHost.isNotBlank() && form.smtpPort.toIntOrNull() != null && form.secret.isNotEmpty()
    KiddoScreen(
        modifier,
        header = { NavBar(title = stringResource(Res.string.smtp_title), onBack = actions.onBack) },
        footer = { Primary(stringResource(Res.string.mail_test_connect), actions.onSubmit, enabled = ready, busy = form.testing) },
    ) {
        Field(form.address, { v -> actions.onEdit { it.copy(address = v) } }, label = stringResource(Res.string.smtp_address), kind = FieldKind.Email)
        SectionLabel(stringResource(Res.string.smtp_sending))
        Field(form.smtpHost, { v -> actions.onEdit { it.copy(smtpHost = v) } }, label = stringResource(Res.string.smtp_server), kind = FieldKind.Url, placeholder = "mail.example.com")
        Row {
            Box(Modifier.weight(1f)) {
                Field(form.smtpPort, { v -> actions.onEdit { it.copy(smtpPort = v.filter(Char::isDigit)) } }, label = stringResource(Res.string.smtp_port), kind = FieldKind.Number)
            }
            Box(Modifier.weight(1f)) {
                PickerField(securityLabel(form.security), onPickSecurity, label = stringResource(Res.string.smtp_security))
            }
        }
        Field(form.username, { v -> actions.onEdit { it.copy(username = v) } }, label = stringResource(Res.string.smtp_username), kind = FieldKind.Email)
        Field(form.secret, { v -> actions.onEdit { it.copy(secret = v) } }, label = stringResource(Res.string.smtp_password), kind = FieldKind.Secret, error = failureText(form))
        SectionLabel(stringResource(Res.string.smtp_reading))
        Field(
            form.imapHost,
            { v -> actions.onEdit { it.copy(imapHost = v) } },
            label = stringResource(Res.string.smtp_server),
            kind = FieldKind.Url,
            placeholder = "mail.example.com",
            note = stringResource(Res.string.smtp_imap_note),
        )
        Note(stringResource(Res.string.smtp_encrypted))
    }
}

@Composable
fun securityLabel(s: MailSecurity) = stringResource(if (s == MailSecurity.SslTls) Res.string.security_ssl else Res.string.security_starttls)

@Composable
private fun SectionLabel(text: String) {
    BasicText(text, style = Kiddo.type.bodyStrong.copy(color = Kiddo.colors.fg), modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 6.dp, bottom = 8.dp))
}

@Composable
fun SendlyScreen(form: ConnectForm, onOpenSendly: () -> Unit, actions: FormActions, modifier: Modifier = Modifier) {
    val ready = form.sendlyFrom.contains('@') && form.secret.isNotBlank()
    KiddoScreen(
        modifier,
        header = { NavBar(onBack = actions.onBack) },
        footer = { Primary(stringResource(Res.string.sendly_connect), actions.onSubmit, enabled = ready, busy = form.testing) },
    ) {
        Box(Modifier.padding(start = 20.dp, top = 4.dp, bottom = 8.dp)) { BrandLogoImage(BrandLogo.Sendly, 40.dp) }
        Title(stringResource(Res.string.sendly_title), sub = stringResource(Res.string.sendly_sub))
        Group {
            ListRow(
                stringResource(Res.string.sendly_open),
                lead = { BrandLogoImage(BrandLogo.Sendly, 30.dp) },
                sub = sub(stringResource(Res.string.sendly_open_sub)),
                external = true,
                onClick = onOpenSendly,
            )
        }
        Field(form.secret, { v -> actions.onEdit { it.copy(secret = v.trim()) } }, label = stringResource(Res.string.sendly_key), kind = FieldKind.Secret, error = failureText(form))
        Field(
            form.sendlyFrom,
            { v -> actions.onEdit { it.copy(sendlyFrom = v) } },
            label = stringResource(Res.string.sendly_from),
            kind = FieldKind.Email,
            note = stringResource(Res.string.sendly_from_note),
        )
        Note(stringResource(Res.string.sendly_no_attach), glyph = KiddoGlyph.Info)
    }
}

private const val APP_PASSWORD = 16
