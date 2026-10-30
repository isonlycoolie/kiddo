package app.kiddo.feature.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.kiddo.core.designsystem.Kiddo
import app.kiddo.core.designsystem.component.GlowSpark
import app.kiddo.core.designsystem.component.KiddoIcon
import app.kiddo.core.designsystem.component.Pill
import app.kiddo.core.designsystem.component.Spinner
import app.kiddo.core.designsystem.component.pressable
import app.kiddo.core.model.CvFormat
import app.kiddo.core.platform.CvPage
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.blocked_open
import app.kiddo.core.resources.blocked_paste
import app.kiddo.core.resources.cvcard_by
import app.kiddo.core.resources.cvcard_open
import app.kiddo.core.resources.cvcard_save
import app.kiddo.core.resources.cvcard_template
import app.kiddo.core.resources.draft_edit
import app.kiddo.core.resources.draft_mark_applied
import app.kiddo.core.resources.draft_review
import app.kiddo.core.resources.draft_sent
import app.kiddo.core.resources.draft_subject
import app.kiddo.core.resources.draft_to
import app.kiddo.core.resources.draft_view
import app.kiddo.core.resources.generated.KiddoGlyph
import app.kiddo.core.resources.job_apply_site
import app.kiddo.core.resources.job_apply_to
import app.kiddo.core.resources.job_closes
import app.kiddo.core.resources.job_label
import app.kiddo.core.resources.job_missing
import app.kiddo.feature.chat.agent.CvBuiltCard
import app.kiddo.feature.chat.agent.DraftCard
import app.kiddo.feature.chat.agent.JobCard
import app.kiddo.feature.chat.agent.StepCard
import app.kiddo.feature.chat.agent.StepState
import org.jetbrains.compose.resources.stringResource

class CardActions(
    val onEditDraft: (String) -> Unit,
    val onReviewDraft: (String, DraftCard) -> Unit,
    val onMarkApplied: (String) -> Unit,
    val onOpenSite: (String) -> Unit,
    val onViewApplication: (String) -> Unit,
    val onOpenBrowser: (String) -> Unit,
    val onPaste: () -> Unit,
    val onSaveCv: (String) -> Unit,
    val onOpenCv: (String) -> Unit,
    val renderCv: suspend (CvBuiltCard) -> ByteArray,
)

// One thing Kiddo did: a grey line with a green check, a turning ring while running, a red alert when it failed.
@Composable
fun StepLine(step: StepCard, modifier: Modifier = Modifier) {
    val colors = Kiddo.colors
    Row(modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        when (step.state) {
            StepState.Done -> KiddoIcon(KiddoGlyph.Check, null, size = 16.dp, stroke = 2.2f, tint = colors.ok)
            StepState.Failed -> KiddoIcon(KiddoGlyph.Alert, null, size = 16.dp, tint = colors.danger)
            StepState.Running -> Spinner(size = 14.dp)
        }
        BasicText(
            step.text,
            style = Kiddo.type.sub.copy(color = if (step.state == StepState.Failed) colors.danger else colors.muted),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

