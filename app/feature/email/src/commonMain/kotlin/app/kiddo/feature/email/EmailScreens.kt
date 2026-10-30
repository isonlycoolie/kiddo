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

@Composable
fun EmailScreen(account: MailAccount?, actions: EmailActions, modifier: Modifier = Modifier) {
    KiddoScreen(modifier, header = { NavBar(title = stringResource(Res.string.email_title), onBack = actions.onBack) }) {
        if (account != null) {
            Group(title = stringResource(Res.string.email_sending_from)) {
                val what = stringResource(if (account.readsReplies) Res.string.email_reads_replies else Res.string.email_sends_only)
                ListRow(account.address, lead = { MailLogo(account.kind) }, sub = connected(stringResource(Res.string.email_connected), what), onClick = actions.onAccount)
            }
        }
        Group(title = stringResource(if (account == null) Res.string.email_connect_way else Res.string.email_connect_other)) {
            ListRow(stringResource(Res.string.email_gmail), lead = { MailLogo(MailKind.GmailAppPassword) }, sub = sub(stringResource(Res.string.email_gmail_sub)), onClick = actions.onGmail)
            Hairline()
            ListRow(
                stringResource(Res.string.email_google),
                lead = { LogoTile { BrandLogoImage(BrandLogo.Google, 19.dp) } },
                sub = sub(stringResource(Res.string.email_google_sub)),
                onClick = actions.onGoogle,
            )
            Hairline()
            ListRow(stringResource(Res.string.email_domain), lead = { MailLogo(MailKind.Smtp) }, sub = sub(stringResource(Res.string.email_domain_sub)), onClick = actions.onSmtp)
            Hairline()
            ListRow(stringResource(Res.string.email_sendly), lead = { MailLogo(MailKind.Sendly) }, sub = sub(stringResource(Res.string.email_sendly_sub)), onClick = actions.onSendly)
        }
    }
}

@Composable
private fun connected(word: String, rest: String): AnnotatedString {
    val ok = Kiddo.colors.ok
    return buildAnnotatedString {
        withStyle(SpanStyle(color = ok)) { append(word) }
        append(" · $rest")
    }
}

// The two steps Google asks for, each opening the right Google page. Step-by-step help lives elsewhere.
@Composable
fun GmailGuideScreen(onBack: () -> Unit, onOpen: (String) -> Unit, onPaste: () -> Unit, modifier: Modifier = Modifier) {
    KiddoScreen(modifier, header = { NavBar(onBack = onBack) }, footer = { Primary(stringResource(Res.string.gmail_paste), onPaste) }) {
        Box(Modifier.padding(start = 20.dp, top = 4.dp, bottom = 4.dp)) { BrandLogoImage(BrandLogo.Gmail, 40.dp) }
        Title(stringResource(Res.string.gmail_title), sub = stringResource(Res.string.gmail_sub))
        Group {
            GuideStep(stringResource(Res.string.gmail_step_2sv), stringResource(Res.string.gmail_step_2sv_body)) { onOpen(TWO_STEP_URL) }
            Hairline()
            GuideStep(stringResource(Res.string.gmail_step_app), stringResource(Res.string.gmail_step_app_body)) { onOpen(APP_PASSWORDS_URL) }
        }
        Note(stringResource(Res.string.gmail_work_hint), glyph = KiddoGlyph.Info)
    }
}

@Composable
private fun GuideStep(title: String, body: String, onOpen: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        TodoCircle()
        Column(Modifier.weight(1f)) {
            BasicText(title, style = Kiddo.type.row.copy(color = Kiddo.colors.fg))
            BasicText(body, style = Kiddo.type.sub.copy(color = Kiddo.colors.muted))
        }
        Box(
            Modifier.height(32.dp).border(1.dp, Kiddo.colors.line, CircleShape).pressable(onOpen, pressedScale = 0.97f).padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) { BasicText(stringResource(Res.string.action_open), style = Kiddo.type.button.copy(color = Kiddo.colors.fg)) }
    }
}

class AccountActions(val onBack: () -> Unit, val onTest: () -> Unit, val onDisconnect: () -> Unit)

@Composable
fun EmailAccountScreen(account: MailAccount?, check: MailCheck?, testing: Boolean, actions: AccountActions, modifier: Modifier = Modifier) {
    KiddoScreen(modifier, header = { NavBar(title = account?.let { kindLabel(it.kind) }, onBack = actions.onBack) }) {
        if (account == null) return@KiddoScreen
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                MailLogo(account.kind, 68.dp)
                Tick(Modifier.align(Alignment.BottomEnd).offset(4.dp, 4.dp), size = 24.dp)
            }
            BasicText(stringResource(Res.string.email_connected), style = Kiddo.type.heading.copy(color = Kiddo.colors.fg), modifier = Modifier.padding(top = 14.dp))
            BasicText(account.address, style = Kiddo.type.body.copy(color = Kiddo.colors.muted))
        }
        Group {
            ListRow(stringResource(Res.string.account_sending), trailing = { Tick() })
            Hairline()
            if (account.readsReplies && check?.reading != false) {
                ListRow(stringResource(Res.string.account_reading), trailing = { Tick() })
            } else {
                ListRow(stringResource(Res.string.account_reading), value = stringResource(Res.string.account_reading_none))
            }
        }
        Box(Modifier.padding(horizontal = 16.dp)) { Secondary(stringResource(Res.string.account_test_mail), actions.onTest, glyph = KiddoGlyph.Sent, busy = testing) }
        TextAction(stringResource(Res.string.account_disconnect), actions.onDisconnect, Modifier.padding(top = 8.dp), danger = true)
    }
}

@Composable
private fun kindLabel(kind: MailKind) = stringResource(
    when (kind) {
        MailKind.GmailAppPassword -> Res.string.email_gmail
        MailKind.Smtp -> Res.string.email_domain
        MailKind.Sendly -> Res.string.email_sendly
    },
)

// Google sign-in needs Google's approval of Kiddo first; until then this points to the app password.
@Composable
fun GoogleSignInScreen(onBack: () -> Unit, onUsePassword: () -> Unit, modifier: Modifier = Modifier) {
    KiddoScreen(modifier, header = { NavBar(onBack = onBack) }, footer = { Primary(stringResource(Res.string.google_use_password), onUsePassword) }) {
        Box(Modifier.padding(start = 20.dp, top = 4.dp, bottom = 8.dp)) { BrandLogoImage(BrandLogo.Google, 36.dp) }
        Title(stringResource(Res.string.google_title), sub = stringResource(Res.string.google_sub))
    }
}

const val TWO_STEP_URL = "https://myaccount.google.com/signinoptions/two-step-verification"
const val APP_PASSWORDS_URL = "https://myaccount.google.com/apppasswords"
const val SENDLY_URL = "https://dashboard.usesendly.cc"
