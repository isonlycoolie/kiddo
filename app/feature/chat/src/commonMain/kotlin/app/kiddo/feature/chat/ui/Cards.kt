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

@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).border(1.dp, Kiddo.colors.line, RoundedCornerShape(18.dp)), content = content)
}

@Composable
private fun Fact(glyph: KiddoGlyph, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        KiddoIcon(glyph, null, Modifier.padding(top = 2.dp), size = 17.dp, tint = Kiddo.colors.muted)
        BasicText(text, style = Kiddo.type.body.copy(color = Kiddo.colors.fg, fontSize = Kiddo.type.button.fontSize))
    }
}

// What Kiddo understood from the post, and what's missing, in red.
@Composable
fun JobCardView(job: JobCard, modifier: Modifier = Modifier) {
    val colors = Kiddo.colors
    Card(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column {
                BasicText(stringResource(Res.string.job_label), style = Kiddo.type.label.copy(color = colors.muted))
                BasicText(job.role, style = Kiddo.type.bodyStrong.copy(color = colors.fg))
                val where = listOf(job.company, job.location).filter { it.isNotBlank() }.joinToString(" · ")
                if (where.isNotEmpty()) BasicText(where, style = Kiddo.type.sub.copy(color = colors.muted))
            }
            if (job.applyEmail.isNotBlank()) Fact(KiddoGlyph.Mail, stringResource(Res.string.job_apply_to, job.applyEmail))
            if (job.applyEmail.isBlank() && job.applyUrl.isNotBlank()) Fact(KiddoGlyph.Globe, job.applyUrl)
            if (job.deadline.isNotBlank()) Fact(KiddoGlyph.Clock, stringResource(Res.string.job_closes, job.deadline))
            if (job.requirements.isNotEmpty()) Fact(KiddoGlyph.Briefcase, job.requirements.take(REQUIREMENTS).joinToString(", "))
        }
        if (job.missing.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().background(colors.danger.copy(alpha = 0.08f)).padding(horizontal = 16.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                KiddoIcon(KiddoGlyph.Alert, null, size = 17.dp, tint = colors.danger)
                BasicText(stringResource(Res.string.job_missing, job.missing.joinToString(", ")), style = Kiddo.type.sub.copy(color = colors.danger, fontWeight = FontWeight.Medium))
            }
        }
    }
}

// The email Kiddo wrote. Edit opens the whole draft; Review and send opens the send sheet. Once sent, a quiet line.
@Composable
fun DraftCardView(id: String, draft: DraftCard, actions: CardActions, modifier: Modifier = Modifier) {
    val colors = Kiddo.colors
    Card(modifier) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            HeaderLine(stringResource(Res.string.draft_to), draft.to.ifBlank { draft.applyUrl })
            HeaderLine(stringResource(Res.string.draft_subject), draft.subject)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
        BasicText(
            draft.body,
            style = Kiddo.type.body.copy(color = colors.fg, fontSize = Kiddo.type.button.fontSize),
            maxLines = PREVIEW_LINES,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(16.dp),
        )
        draft.cvName?.let { name ->
            Row(Modifier.padding(start = 16.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.background(colors.well, RoundedCornerShape(8.dp)).padding(7.dp)) { KiddoIcon(KiddoGlyph.File, null, size = 18.dp) }
                BasicText(name, style = Kiddo.type.button.copy(color = colors.fg))
            }
        }
        if (draft.sent) {
            Row(
                Modifier.fillMaxWidth().border(0.dp, Color.Transparent).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                KiddoIcon(KiddoGlyph.Done, null, size = 18.dp, tint = colors.ok)
                BasicText(stringResource(Res.string.draft_sent), style = Kiddo.type.button.copy(color = colors.fg), modifier = Modifier.weight(1f))
                draft.applicationId?.let { appId ->
                    BasicText(stringResource(Res.string.draft_view), style = Kiddo.type.button.copy(color = colors.fg, fontWeight = FontWeight.SemiBold), modifier = Modifier.pressable({ actions.onViewApplication(appId) }, pressedAlpha = 0.5f))
                }
            }
        } else {
            Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(stringResource(Res.string.draft_edit), { actions.onEditDraft(id) }, outline = true, glyph = KiddoGlyph.NewChat)
                if (draft.to.isNotBlank()) {
                    Pill(stringResource(Res.string.draft_review), { actions.onReviewDraft(id, draft) }, Modifier.weight(1f))
                } else if (draft.applyUrl.isNotBlank()) {
                    Pill(stringResource(Res.string.job_apply_site), { actions.onOpenSite(draft.applyUrl) }, Modifier.weight(1f))
                }
            }
            if (draft.to.isBlank()) {
                Pill(stringResource(Res.string.draft_mark_applied), { actions.onMarkApplied(id) }, Modifier.padding(start = 16.dp, bottom = 14.dp), outline = true)
            }
        }
    }
}

@Composable
private fun HeaderLine(name: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        BasicText(name, style = Kiddo.type.sub.copy(color = Kiddo.colors.muted), modifier = Modifier.width(56.dp))
        BasicText(value, style = Kiddo.type.sub.copy(color = Kiddo.colors.fg), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun BlockedView(url: String, actions: CardActions, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Pill(stringResource(Res.string.blocked_open), { actions.onOpenBrowser(url) }, glyph = KiddoGlyph.Globe)
        Pill(stringResource(Res.string.blocked_paste), actions.onPaste, outline = true)
    }
}

// A CV Kiddo made: the new page fills the card, a dark fade at the foot keeps the name and buttons readable.
@Composable
fun CvBuiltView(id: String, card: CvBuiltCard, actions: CardActions, modifier: Modifier = Modifier) {
    var pdf by remember(card) { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(card) { pdf = actions.renderCv(card) }
    Box(modifier.fillMaxWidth().height(320.dp).clip(RoundedCornerShape(20.dp)).border(1.dp, Kiddo.colors.line, RoundedCornerShape(20.dp))) {
        pdf?.let { CvPage(it, CvFormat.Pdf, null, Modifier.fillMaxWidth()) }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f), Color.Black.copy(alpha = 0.95f))))
                .padding(start = 16.dp, end = 16.dp, top = 96.dp, bottom = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GlowSpark(16.dp)
                BasicText(stringResource(Res.string.cvcard_by), style = Kiddo.type.label.copy(color = Color.White.copy(alpha = 0.75f)))
            }
            BasicText(card.cv.name, style = Kiddo.type.bodyStrong.copy(color = Color.White), modifier = Modifier.padding(top = 2.dp))
            BasicText(stringResource(Res.string.cvcard_template), style = Kiddo.type.sub.copy(color = Color.White.copy(alpha = 0.7f)))
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val saved = card.savedCvId
                val label = stringResource(if (saved != null) Res.string.cvcard_open else Res.string.cvcard_save)
                Box(
                    Modifier.weight(1f).height(36.dp).background(Color.White, CircleShape)
                        .pressable({ if (saved != null) actions.onOpenCv(saved) else actions.onSaveCv(id) }, pressedScale = 0.98f),
                    contentAlignment = Alignment.Center,
                ) { BasicText(label, style = Kiddo.type.button.copy(color = Color.Black, fontWeight = FontWeight.SemiBold)) }
            }
        }
    }
}

private const val REQUIREMENTS = 4
private const val PREVIEW_LINES = 5
