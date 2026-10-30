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

