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

