package app.kiddo.feature.email

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.kiddo.core.designsystem.Kiddo
import app.kiddo.core.designsystem.component.BrandLogoImage
import app.kiddo.core.designsystem.component.Group
import app.kiddo.core.designsystem.component.Hairline
import app.kiddo.core.designsystem.component.KiddoIcon
import app.kiddo.core.designsystem.component.KiddoScreen
import app.kiddo.core.designsystem.component.ListRow
import app.kiddo.core.designsystem.component.LogoTile
import app.kiddo.core.designsystem.component.NavBar
import app.kiddo.core.designsystem.component.Note
import app.kiddo.core.designsystem.component.Primary
import app.kiddo.core.designsystem.component.Secondary
import app.kiddo.core.designsystem.component.TextAction
import app.kiddo.core.designsystem.component.Tick
import app.kiddo.core.designsystem.component.Title
import app.kiddo.core.designsystem.component.TodoCircle
import app.kiddo.core.designsystem.component.pressable
import app.kiddo.core.designsystem.component.sub
import app.kiddo.core.mail.MailCheck
import app.kiddo.core.model.MailAccount
import app.kiddo.core.model.MailKind
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.account_disconnect
import app.kiddo.core.resources.account_reading
import app.kiddo.core.resources.account_reading_none
import app.kiddo.core.resources.account_sending
import app.kiddo.core.resources.account_test_mail
import app.kiddo.core.resources.action_open
import app.kiddo.core.resources.email_connect_other
import app.kiddo.core.resources.email_connect_way
import app.kiddo.core.resources.email_connected
import app.kiddo.core.resources.email_domain
import app.kiddo.core.resources.email_domain_sub
import app.kiddo.core.resources.email_gmail
import app.kiddo.core.resources.email_gmail_sub
import app.kiddo.core.resources.email_google
import app.kiddo.core.resources.email_google_sub
import app.kiddo.core.resources.email_reads_replies
import app.kiddo.core.resources.email_sending_from
import app.kiddo.core.resources.email_sendly
import app.kiddo.core.resources.email_sendly_sub
import app.kiddo.core.resources.email_sends_only
import app.kiddo.core.resources.email_title
import app.kiddo.core.resources.generated.BrandLogo
import app.kiddo.core.resources.generated.KiddoGlyph
import app.kiddo.core.resources.gmail_paste
import app.kiddo.core.resources.gmail_step_2sv
import app.kiddo.core.resources.gmail_step_2sv_body
import app.kiddo.core.resources.gmail_step_app
import app.kiddo.core.resources.gmail_step_app_body
import app.kiddo.core.resources.gmail_sub
import app.kiddo.core.resources.gmail_title
import app.kiddo.core.resources.gmail_work_hint
import app.kiddo.core.resources.google_sub
import app.kiddo.core.resources.google_title
import app.kiddo.core.resources.google_use_password
import org.jetbrains.compose.resources.stringResource

@Composable
fun MailLogo(kind: MailKind, size: androidx.compose.ui.unit.Dp = 36.dp) {
    when (kind) {
        MailKind.Sendly -> BrandLogoImage(BrandLogo.Sendly, size)
        MailKind.GmailAppPassword -> LogoTile(size = size) { BrandLogoImage(BrandLogo.Gmail, size * 0.6f) }
        MailKind.Smtp -> LogoTile(size = size) { KiddoIcon(KiddoGlyph.Server, null, size = size * 0.55f) }
    }
}

class EmailActions(val onBack: () -> Unit, val onAccount: () -> Unit, val onGmail: () -> Unit, val onGoogle: () -> Unit, val onSmtp: () -> Unit, val onSendly: () -> Unit)

