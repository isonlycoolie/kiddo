package app.kiddo.feature.cvs

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.kiddo.core.designsystem.Kiddo
import app.kiddo.core.designsystem.component.Empty
import app.kiddo.core.designsystem.component.IconButton
import app.kiddo.core.designsystem.component.KiddoScreen
import app.kiddo.core.designsystem.component.NavBar
import app.kiddo.core.designsystem.component.Pill
import app.kiddo.core.designsystem.component.Primary
import app.kiddo.core.designsystem.component.Spinner
import app.kiddo.core.designsystem.component.pressable
import app.kiddo.core.model.Cv
import app.kiddo.core.platform.CvPage
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.action_more
import app.kiddo.core.resources.cv_improve
import app.kiddo.core.resources.cv_no_text
import app.kiddo.core.resources.cvs_add
import app.kiddo.core.resources.cvs_by_kiddo
import app.kiddo.core.resources.cvs_choose
import app.kiddo.core.resources.cvs_default
import app.kiddo.core.resources.cvs_empty_body
import app.kiddo.core.resources.cvs_empty_title
import app.kiddo.core.resources.cvs_pages
import app.kiddo.core.resources.cvs_pages_many
import app.kiddo.core.resources.cvs_title
import app.kiddo.core.resources.generated.KiddoGlyph
import org.jetbrains.compose.resources.stringResource

@Composable
fun CvsScreen(
    cvs: List<Cv>?,
    files: Map<String, ByteArray>,
    busy: Boolean,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
    onNeedFile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    KiddoScreen(
        modifier,
        header = {
            NavBar(title = stringResource(Res.string.cvs_title), onBack = onBack) {
                if (busy) Box(Modifier.padding(13.dp)) { Spinner() } else if (!cvs.isNullOrEmpty()) IconButton(KiddoGlyph.Add, stringResource(Res.string.cvs_add), onAdd)
            }
        },
    ) {
        when {
            cvs == null -> Unit
            cvs.isEmpty() -> Empty(KiddoGlyph.File, stringResource(Res.string.cvs_empty_title), body = stringResource(Res.string.cvs_empty_body)) {
                Pill(stringResource(Res.string.cvs_choose), onAdd, glyph = KiddoGlyph.Upload)
            }
            else -> Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                cvs.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        pair.forEach { cv ->
                            LaunchedEffect(cv.id) { onNeedFile(cv.id) }
                            CvCard(cv, files[cv.id], { onOpen(cv.id) }, Modifier.weight(1f))
                        }
                        if (pair.size == 1) Box(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

