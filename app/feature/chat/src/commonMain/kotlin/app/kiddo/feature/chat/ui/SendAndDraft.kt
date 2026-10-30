package app.kiddo.feature.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.kiddo.core.designsystem.Kiddo
import app.kiddo.core.designsystem.component.Field
import app.kiddo.core.designsystem.component.FieldKind
import app.kiddo.core.designsystem.component.Hairline
import app.kiddo.core.designsystem.component.IconButton
import app.kiddo.core.designsystem.component.KiddoIcon
import app.kiddo.core.designsystem.component.KiddoScreen
import app.kiddo.core.designsystem.component.NavBar
import app.kiddo.core.designsystem.component.Primary
import app.kiddo.core.designsystem.component.SheetGroup
import app.kiddo.core.model.MailAccount
import app.kiddo.core.model.MailKind
import app.kiddo.core.platform.KiddoBrowser
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.action_close
import app.kiddo.core.resources.action_save
import app.kiddo.core.resources.browser_forget
import app.kiddo.core.resources.browser_use
import app.kiddo.core.resources.draft_body
import app.kiddo.core.resources.draft_subject
import app.kiddo.core.resources.draft_title
import app.kiddo.core.resources.draft_to
import app.kiddo.core.resources.generated.KiddoGlyph
import app.kiddo.core.resources.send_attached
import app.kiddo.core.resources.send_follow_note
import app.kiddo.core.resources.send_from
import app.kiddo.core.resources.send_from_page
import app.kiddo.core.resources.send_no_attach
import app.kiddo.core.resources.send_send
import app.kiddo.feature.chat.agent.DraftCard
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

// The send sheet: the real From, To, Subject and attachment, a red line when the address came from the page, and
// one Send. This is the only way an application leaves the phone.
@Composable
fun SendSheet(draft: DraftCard, mail: MailAccount, followUpDays: Int, onSend: (onDone: () -> Unit) -> Unit) {
    var sending by remember { mutableStateOf(false) }
    SheetGroup {
        SendLine(stringResource(Res.string.send_from), mail.address)
        Hairline()
        SendLine(stringResource(Res.string.draft_to), draft.to)
        Hairline()
        SendLine(stringResource(Res.string.draft_subject), draft.subject)
        Hairline()
        val attached = when {
            mail.kind == MailKind.Sendly -> stringResource(Res.string.send_no_attach)
            else -> draft.cvName ?: "-"
        }
        SendLine(stringResource(Res.string.send_attached), attached)
    }
    if (draft.fromPage) {
        Row(Modifier.padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KiddoIcon(KiddoGlyph.Alert, null, size = 17.dp, tint = Kiddo.colors.danger)
            BasicText(stringResource(Res.string.send_from_page), style = Kiddo.type.sub.copy(color = Kiddo.colors.danger))
        }
    }
    BasicText(stringResource(Res.string.send_follow_note, followUpDays), style = Kiddo.type.sub.copy(color = Kiddo.colors.muted), modifier = Modifier.padding(horizontal = 4.dp))
    Primary(
        stringResource(Res.string.send_send),
        {
            sending = true
            onSend { sending = false }
        },
        glyph = KiddoGlyph.Sent,
        busy = sending,
    )
}

@Composable
private fun SendLine(name: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BasicText(name, style = Kiddo.type.body.copy(color = Kiddo.colors.muted), modifier = Modifier.width(72.dp))
        BasicText(value, style = Kiddo.type.body.copy(color = Kiddo.colors.fg), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

// The whole draft, editable like a mail app.
@Composable
fun DraftScreen(draft: DraftCard, onBack: () -> Unit, onSave: (to: String, subject: String, body: String) -> Unit, modifier: Modifier = Modifier) {
    var to by remember(draft) { mutableStateOf(draft.to) }
    var subject by remember(draft) { mutableStateOf(draft.subject) }
    var body by remember(draft) { mutableStateOf(draft.body) }
    KiddoScreen(
        modifier,
        header = { NavBar(title = stringResource(Res.string.draft_title), onBack = onBack) },
        footer = { Primary(stringResource(Res.string.action_save), { onSave(to, subject, body) }, enabled = subject.isNotBlank() && body.isNotBlank()) },
    ) {
        if (draft.to.isNotBlank()) Field(to, { to = it }, label = stringResource(Res.string.draft_to), kind = FieldKind.Email)
        Field(subject, { subject = it }, label = stringResource(Res.string.draft_subject))
        Field(body, { body = it }, label = stringResource(Res.string.draft_body), singleLine = false)
    }
}

// The Kiddo browser: the page as the user sees it, and one button that hands it to Kiddo.
@Composable
fun BrowserScreen(url: String, onClose: () -> Unit, onUse: (String) -> Unit, modifier: Modifier = Modifier) {
    var reader by remember { mutableStateOf<(suspend () -> String)?>(null) }
    var reading by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    KiddoScreen(
        modifier,
        scrolls = false,
        header = {
            Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(KiddoGlyph.Close, stringResource(Res.string.action_close), onClose)
                Row(
                    Modifier.weight(1f).height(36.dp).background(Kiddo.colors.well, CircleShape).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                ) {
                    KiddoIcon(KiddoGlyph.Lock, null, size = 14.dp, tint = Kiddo.colors.ok)
                    BasicText(url.substringAfter("://").substringBefore('/'), style = Kiddo.type.button.copy(color = Kiddo.colors.fg), maxLines = 1)
                }
                Box(Modifier.width(44.dp))
            }
        },
        footer = {
            Primary(stringResource(Res.string.browser_use), {
                val read = reader ?: return@Primary
                reading = true
                scope.launch {
                    val text = read()
                    reading = false
                    onUse(text)
                }
            }, enabled = reader != null, busy = reading)
            BasicText(
                stringResource(Res.string.browser_forget),
                style = Kiddo.type.small.copy(color = Kiddo.colors.muted, textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        },
    ) {
        Column(Modifier.weight(1f).fillMaxWidth()) { KiddoBrowser(url, { reader = it }, Modifier.fillMaxSize()) }
    }
}
