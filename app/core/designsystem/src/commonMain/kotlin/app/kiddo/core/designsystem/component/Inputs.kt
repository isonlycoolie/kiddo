package app.kiddo.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.kiddo.core.designsystem.Kiddo
import app.kiddo.core.designsystem.KiddoMotion
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.action_hide
import app.kiddo.core.resources.action_show
import app.kiddo.core.resources.generated.KiddoGlyph
import org.jetbrains.compose.resources.stringResource

enum class FieldKind { Text, Email, Url, Number, Secret }

// Field: a label over a grey well that holds the value. Secrets are masked with a show/hide eye and never learned by
// the keyboard. An error line replaces the note in red.
@Composable
fun Field(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    kind: FieldKind = FieldKind.Text,
    note: String? = null,
    error: String? = null,
    lead: (@Composable () -> Unit)? = null,
    end: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
) {
    val colors = Kiddo.colors
    var shown by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
        label?.let {
            BasicText(it, style = Kiddo.type.label.copy(color = colors.muted), modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 50.dp).background(colors.well, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            lead?.let {
                it()
                Box(Modifier.size(10.dp))
            }
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) BasicText(placeholder, style = Kiddo.type.body.copy(color = colors.muted), maxLines = 1)
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    singleLine = singleLine,
                    textStyle = Kiddo.type.body.copy(color = colors.fg),
                    cursorBrush = SolidColor(colors.fg),
                    visualTransformation = if (kind == FieldKind.Secret && !shown) PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = keyboard(kind),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (kind == FieldKind.Secret && value.isNotEmpty()) {
                val label = stringResource(if (shown) Res.string.action_hide else Res.string.action_show)
                KiddoIcon(
                    if (shown) KiddoGlyph.Hide else KiddoGlyph.View,
                    label,
                    Modifier.padding(start = 8.dp).pressable({ shown = !shown }, pressedAlpha = 0.5f, label = label),
                    size = 20.dp,
                    tint = colors.muted,
                )
            }
            end?.let {
                Box(Modifier.size(8.dp))
                it()
            }
        }
        val line = error ?: note
        if (line != null) {
            BasicText(
                line,
                style = Kiddo.type.small.copy(color = if (error != null) colors.danger else colors.muted),
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 6.dp),
            )
        }
    }
}

private fun keyboard(kind: FieldKind) = when (kind) {
    FieldKind.Text -> KeyboardOptions(imeAction = ImeAction.Next)
    FieldKind.Email -> KeyboardOptions(keyboardType = KeyboardType.Email, autoCorrectEnabled = false, imeAction = ImeAction.Next)
    FieldKind.Url -> KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false, imeAction = ImeAction.Next)
    FieldKind.Number -> KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
    FieldKind.Secret -> KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false, imeAction = ImeAction.Done)
}

// PickerField: looks like a Field but opens a sheet to choose (provider, model, security). Never shows a keyboard.
@Composable
fun PickerField(
    value: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    lead: (@Composable () -> Unit)? = null,
    note: String? = null,
) {
    val colors = Kiddo.colors
    Column(modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
        label?.let {
            BasicText(it, style = Kiddo.type.label.copy(color = colors.muted), modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 50.dp).background(colors.well, RoundedCornerShape(14.dp))
                .pressable(onClick, pressedAlpha = 0.7f, role = Role.DropdownList, label = label)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            lead?.let {
                it()
                Box(Modifier.size(10.dp))
            }
            BasicText(
                value.ifEmpty { placeholder },
                style = Kiddo.type.body.copy(color = if (value.isEmpty()) colors.muted else colors.fg),
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            if (onClick != null) KiddoIcon(KiddoGlyph.Down, null, size = 16.dp, tint = colors.muted)
        }
        note?.let {
            BasicText(it, style = Kiddo.type.small.copy(color = colors.muted), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 6.dp))
        }
    }
}

// Toggle: on is the foreground colour, like the design; the knob slides.
@Composable
fun Toggle(on: Boolean, onChange: (Boolean) -> Unit, label: String, modifier: Modifier = Modifier) {
    val colors = Kiddo.colors
    val spec = if (Kiddo.reducedMotion) KiddoMotion.reduced() else KiddoMotion.tween<androidx.compose.ui.unit.Dp>(200, KiddoMotion.Ease)
    val knob by animateDpAsState(if (on) 20.dp else 0.dp, spec, label = "knob")
    val track by animateColorAsState(if (on) colors.fg else colors.raised, KiddoMotion.tween<Color>(200, KiddoMotion.Ease), label = "track")
    Box(modifier.size(50.dp, 30.dp).background(track, CircleShape).pressable({ onChange(!on) }, role = Role.Switch, label = label)) {
        Box(
            Modifier.offset(3.dp, 3.dp).graphicsLayer { translationX = knob.toPx() }
                .shadow(2.dp, CircleShape).size(24.dp).background(colors.bg, CircleShape),
        )
    }
}
