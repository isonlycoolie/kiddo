package app.kiddo.feature.applications

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.kiddo.core.designsystem.Kiddo
import app.kiddo.core.designsystem.component.Empty
import app.kiddo.core.designsystem.component.Group
import app.kiddo.core.designsystem.component.Hairline
import app.kiddo.core.designsystem.component.IconButton
import app.kiddo.core.designsystem.component.KiddoScreen
import app.kiddo.core.designsystem.component.ListRow
import app.kiddo.core.designsystem.component.NavBar
import app.kiddo.core.designsystem.component.Pill
import app.kiddo.core.designsystem.component.Primary
import app.kiddo.core.designsystem.component.Secondary
import app.kiddo.core.designsystem.component.pressable
import app.kiddo.core.designsystem.component.sub
import app.kiddo.core.model.AppStatus
import app.kiddo.core.model.Application
import app.kiddo.core.platform.shortDate
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.action_more
import app.kiddo.core.resources.app_applied_email
import app.kiddo.core.resources.app_applied_site
import app.kiddo.core.resources.app_cv_sent
import app.kiddo.core.resources.app_event_follow
import app.kiddo.core.resources.app_event_reply
import app.kiddo.core.resources.app_event_sent
import app.kiddo.core.resources.app_event_waiting
import app.kiddo.core.resources.app_follow_now
import app.kiddo.core.resources.app_happened
import app.kiddo.core.resources.app_open_chat
import app.kiddo.core.resources.app_their_reply
import app.kiddo.core.resources.apps_all
import app.kiddo.core.resources.apps_closed
import app.kiddo.core.resources.apps_empty
import app.kiddo.core.resources.apps_empty_body
import app.kiddo.core.resources.apps_replied
import app.kiddo.core.resources.apps_start_chat
import app.kiddo.core.resources.apps_title
import app.kiddo.core.resources.apps_waiting
import app.kiddo.core.resources.generated.KiddoGlyph
import app.kiddo.core.resources.status_closed
import app.kiddo.core.resources.status_interview
import app.kiddo.core.resources.status_replied
import app.kiddo.core.resources.status_sent
import app.kiddo.core.resources.status_site
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

fun statusLabel(s: AppStatus): StringResource = when (s) {
    AppStatus.Sent -> Res.string.status_sent
    AppStatus.Replied -> Res.string.status_replied
    AppStatus.Interview -> Res.string.status_interview
    AppStatus.AppliedOnSite -> Res.string.status_site
    AppStatus.Closed -> Res.string.status_closed
}

// Soft tints: good news green, closed grey, the rest neutral.
@Composable
fun StatusPill(status: AppStatus, modifier: Modifier = Modifier) {
    val c = Kiddo.colors
    val (bg, fg) = when (status) {
        AppStatus.Replied, AppStatus.Interview -> c.ok.copy(alpha = 0.14f) to c.ok
        AppStatus.Closed -> Color.Transparent to c.muted
        else -> c.well to c.fg
    }
    BasicText(
        stringResource(statusLabel(status)),
        style = Kiddo.type.small.copy(color = fg, fontWeight = FontWeight.Medium),
        modifier = modifier.background(bg, CircleShape).padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

private enum class Filter(val label: StringResource, val keep: (AppStatus) -> Boolean) {
    All(Res.string.apps_all, { true }),
    Waiting(Res.string.apps_waiting, { it == AppStatus.Sent || it == AppStatus.AppliedOnSite }),
    Replied(Res.string.apps_replied, { it == AppStatus.Replied || it == AppStatus.Interview }),
    Closed(Res.string.apps_closed, { it == AppStatus.Closed }),
}

@Composable
fun ApplicationsScreen(list: List<Application>?, onBack: () -> Unit, onOpen: (String) -> Unit, onStart: () -> Unit, modifier: Modifier = Modifier) {
    var filter by rememberSaveable { mutableStateOf(Filter.All) }
    KiddoScreen(modifier, header = { NavBar(title = stringResource(Res.string.apps_title), onBack = onBack) }) {
        if (list == null) return@KiddoScreen
        if (list.isEmpty()) {
            Empty(KiddoGlyph.Briefcase, stringResource(Res.string.apps_empty), body = stringResource(Res.string.apps_empty_body)) {
                Pill(stringResource(Res.string.apps_start_chat), onStart)
            }
            return@KiddoScreen
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Filter.entries.forEach { f ->
                val on = f == filter
                BasicText(
                    stringResource(f.label),
                    style = Kiddo.type.button.copy(color = if (on) Kiddo.colors.bg else Kiddo.colors.fg),
                    modifier = Modifier.background(if (on) Kiddo.colors.fg else Kiddo.colors.well, CircleShape)
                        .pressable({ filter = f }, pressedScale = 0.97f).padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
        list.filter { filter.keep(it.status) }.forEach { app ->
            Row(
                Modifier.fillMaxWidth().pressable({ onOpen(app.id) }, pressedAlpha = 0.6f).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CompanyTile(app.company.ifBlank { app.to })
                Column(Modifier.weight(1f)) {
                    BasicText(app.role.ifBlank { app.to }, style = Kiddo.type.row.copy(color = Kiddo.colors.fg), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    BasicText("${app.company} · ${shortDate(app.sentAt)}", style = Kiddo.type.sub.copy(color = Kiddo.colors.muted), maxLines = 1)
                }
                StatusPill(app.status)
            }
        }
    }
}

@Composable
private fun CompanyTile(name: String, size: Int = 44) {
    val initials = name.split(' ', '.', '@').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1).uppercase() }
    Box(Modifier.size(size.dp).background(Kiddo.colors.well, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
        BasicText(initials, style = Kiddo.type.button.copy(color = Kiddo.colors.fg, fontWeight = FontWeight.SemiBold))
    }
}

class ApplicationActions(val onBack: () -> Unit, val onMore: () -> Unit, val onOpenChat: (String) -> Unit, val onFollowUp: () -> Unit)

