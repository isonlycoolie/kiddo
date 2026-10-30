package app.kiddo.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.kiddo.core.designsystem.Kiddo
import app.kiddo.core.resources.generated.KiddoGlyph

// One line in a sheet: an optional glyph or logo, the label with an optional grey line, a check when picked.
@Composable
fun SheetRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glyph: KiddoGlyph? = null,
    lead: (@Composable () -> Unit)? = null,
    sub: String? = null,
    picked: Boolean = false,
    danger: Boolean = false,
) {
    val ink = if (danger) Kiddo.colors.danger else Kiddo.colors.fg
    Row(
        modifier.fillMaxWidth().heightIn(min = 52.dp).pressable(onClick, pressedAlpha = 0.6f, role = Role.Button).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        glyph?.let { KiddoIcon(it, null, size = 21.dp, tint = ink) }
        lead?.invoke()
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            BasicText(label, style = Kiddo.type.row.copy(color = ink))
            sub?.let { BasicText(it, style = Kiddo.type.sub.copy(color = Kiddo.colors.muted)) }
        }
        if (picked) KiddoIcon(KiddoGlyph.Check, null, size = 20.dp, stroke = 2.2f)
    }
}

// A confirm line under a sheet title.
@Composable
fun SheetText(text: String, modifier: Modifier = Modifier) {
    BasicText(
        text,
        modifier.fillMaxWidth().padding(horizontal = 4.dp),
        style = Kiddo.type.body.copy(color = Kiddo.colors.muted, textAlign = TextAlign.Center),
    )
}
