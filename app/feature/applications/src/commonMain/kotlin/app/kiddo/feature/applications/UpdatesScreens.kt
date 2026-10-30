package app.kiddo.feature.applications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.kiddo.core.designsystem.Kiddo
import app.kiddo.core.designsystem.component.Empty
import app.kiddo.core.designsystem.component.Field
import app.kiddo.core.designsystem.component.Group
import app.kiddo.core.designsystem.component.Hairline
import app.kiddo.core.designsystem.component.KiddoIcon
import app.kiddo.core.designsystem.component.KiddoScreen
import app.kiddo.core.designsystem.component.ListRow
import app.kiddo.core.designsystem.component.NavBar
import app.kiddo.core.designsystem.component.Note
import app.kiddo.core.designsystem.component.Primary
import app.kiddo.core.designsystem.component.Spinner
import app.kiddo.core.designsystem.component.Toggle
import app.kiddo.core.designsystem.component.pressable
import app.kiddo.core.model.AppSettings
import app.kiddo.core.model.Notice
import app.kiddo.core.model.NoticeKind
import app.kiddo.core.platform.shortDate
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.follow_count
import app.kiddo.core.resources.follow_send
import app.kiddo.core.resources.follow_title
import app.kiddo.core.resources.followups_after
import app.kiddo.core.resources.followups_first
import app.kiddo.core.resources.followups_max
import app.kiddo.core.resources.followups_note
import app.kiddo.core.resources.followups_on
import app.kiddo.core.resources.followups_title
import app.kiddo.core.resources.followups_weekends
import app.kiddo.core.resources.generated.KiddoGlyph
import app.kiddo.core.resources.draft_body
import app.kiddo.core.resources.draft_subject
import app.kiddo.core.resources.draft_to
import app.kiddo.core.resources.updates_check
import app.kiddo.core.resources.updates_empty
import app.kiddo.core.resources.updates_empty_body
import app.kiddo.core.resources.updates_title
import org.jetbrains.compose.resources.stringResource

// Updates: a coloured dot per kind (green good news, amber waiting on you, red something broke); rows with somewhere
// to go show a chevron.
@Composable
fun UpdatesScreen(notes: List<Notice>?, checking: Boolean, onBack: () -> Unit, onCheck: () -> Unit, onOpen: (Notice) -> Unit, modifier: Modifier = Modifier) {
    KiddoScreen(
        modifier,
        header = {
            NavBar(title = stringResource(Res.string.updates_title), onBack = onBack) {
                Box(Modifier.size(44.dp).pressable(if (checking) null else onCheck, pressedAlpha = 0.5f, label = stringResource(Res.string.updates_check)), contentAlignment = Alignment.Center) {
                    if (checking) Spinner() else KiddoIcon(KiddoGlyph.Refresh, null)
                }
            }
        },
    ) {
        if (notes == null) return@KiddoScreen
        if (notes.isEmpty()) {
            Empty(KiddoGlyph.Bell, stringResource(Res.string.updates_empty), body = stringResource(Res.string.updates_empty_body))
            return@KiddoScreen
        }
        notes.forEach { n -> NoticeRow(n, onOpen) }
    }
}

@Composable
private fun NoticeRow(n: Notice, onOpen: (Notice) -> Unit) {
    val c = Kiddo.colors
    val dot = when (n.kind) {
        NoticeKind.Reply -> c.ok
        NoticeKind.FollowUpDue -> c.warn
        NoticeKind.CheckFailed -> c.danger
        NoticeKind.Sent -> Color.Transparent
    }
    val title = n.body.substringBefore('\n')
    val body = n.body.substringAfter('\n', "")
    val opens = n.applicationId != null || n.kind == NoticeKind.CheckFailed
    Row(
        Modifier.fillMaxWidth().pressable(if (opens) ({ onOpen(n) }) else null, pressedAlpha = 0.6f).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.align(Alignment.Top).padding(top = 7.dp).size(8.dp).background(if (n.read && n.kind != NoticeKind.CheckFailed) Color.Transparent else dot, CircleShape))
        Column(Modifier.weight(1f)) {
            Row {
                BasicText(title, style = Kiddo.type.bodyStrong.copy(color = c.fg), modifier = Modifier.weight(1f))
                BasicText(shortDate(n.createdAt), style = Kiddo.type.small.copy(color = c.muted))
            }
            if (body.isNotBlank()) BasicText(body, style = Kiddo.type.sub.copy(color = c.muted))
        }
        if (opens) KiddoIcon(KiddoGlyph.Chevron, null, size = 18.dp, tint = c.faint)
    }
}

// A due follow-up, written and waiting for the user's tap.
@Composable
fun FollowUpScreen(
    to: String,
    subject: String,
    body: String,
    onBody: (String) -> Unit,
    count: Int,
    max: Int,
    sending: Boolean,
    onBack: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KiddoScreen(
        modifier,
        header = { NavBar(title = stringResource(Res.string.follow_title), onBack = onBack) },
        footer = { Primary(stringResource(Res.string.follow_send), onSend, glyph = KiddoGlyph.Sent, busy = sending, enabled = body.isNotBlank()) },
    ) {
        Group {
            ListRow(to, sub = app.kiddo.core.designsystem.component.sub(stringResource(Res.string.draft_to)))
            Hairline()
            ListRow(subject, sub = app.kiddo.core.designsystem.component.sub(stringResource(Res.string.draft_subject)))
        }
        Field(body, onBody, label = stringResource(Res.string.draft_body), singleLine = false)
        Note(stringResource(Res.string.follow_count, count, max), glyph = KiddoGlyph.Info)
    }
}

class FollowUpsActions(val onBack: () -> Unit, val onChange: ((AppSettings) -> AppSettings) -> Unit, val onDays: () -> Unit, val onMax: () -> Unit)

@Composable
fun FollowUpsScreen(s: AppSettings, actions: FollowUpsActions, modifier: Modifier = Modifier) {
    KiddoScreen(modifier, header = { NavBar(title = stringResource(Res.string.followups_title), onBack = actions.onBack) }) {
        val onLabel = stringResource(Res.string.followups_on)
        Group { ListRow(onLabel, trailing = { Toggle(s.followUps, { on -> actions.onChange { it.copy(followUps = on) } }, onLabel) }) }
        if (s.followUps) {
            val weekends = stringResource(Res.string.followups_weekends)
            Group {
                ListRow(stringResource(Res.string.followups_first), value = stringResource(Res.string.followups_after, s.followUpDays), onClick = actions.onDays)
                Hairline()
                ListRow(stringResource(Res.string.followups_max), value = s.followUpMax.toString(), onClick = actions.onMax)
                Hairline()
                ListRow(weekends, trailing = { Toggle(s.skipWeekends, { on -> actions.onChange { it.copy(skipWeekends = on) } }, weekends) })
            }
        }
        Note(stringResource(Res.string.followups_note))
    }
}
