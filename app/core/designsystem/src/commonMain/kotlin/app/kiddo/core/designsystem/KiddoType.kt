package app.kiddo.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.inter_variable
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.Font

// One style per size the design uses.
@Immutable
data class KiddoType(
    val hero: TextStyle,
    val title: TextStyle,
    val heading: TextStyle,
    val nav: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val chat: TextStyle,
    val button: TextStyle,
    val primary: TextStyle,
    val row: TextStyle,
    val sub: TextStyle,
    val label: TextStyle,
    val small: TextStyle,
    val note: TextStyle,
    val wordmark: TextStyle,
)

private val Centered = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

private fun style(
    family: FontFamily,
    size: Float,
    leading: Float = size * 1.4f,
    weight: FontWeight = FontWeight.Normal,
    tracking: TextUnit = TextUnit.Unspecified,
) = TextStyle(
    fontFamily = family,
    fontSize = size.sp,
    lineHeight = leading.sp,
    fontWeight = weight,
    letterSpacing = tracking,
    lineHeightStyle = Centered,
)

private val Tight = (-0.01f).em

fun kiddoType(family: FontFamily) = KiddoType(
    hero = style(family, 32f, 38f, FontWeight.SemiBold, Tight),
    title = style(family, 26f, 32f, FontWeight.SemiBold, Tight),
    heading = style(family, 19f, 25f, FontWeight.SemiBold, Tight),
    nav = style(family, 17f, 22f, FontWeight.SemiBold),
    body = style(family, 16f, 22f),
    bodyStrong = style(family, 16f, 22f, FontWeight.SemiBold),
    chat = style(family, 16f, 25f),
    button = style(family, 14.5f, 20f, FontWeight.Medium),
    primary = style(family, 16f, 22f, FontWeight.SemiBold),
    row = style(family, 16f, 21f),
    sub = style(family, 13.5f, 18f),
    label = style(family, 13.5f, 18f, FontWeight.Medium),
    small = style(family, 13f, 17f),
    note = style(family, 15.5f, 22f),
    wordmark = style(family, 34f, 34f, FontWeight.Bold, (-0.045f).em),
)

// Inter is variable; each weight is one instance of the same file.
@OptIn(ExperimentalResourceApi::class)
@Composable
fun interFamily(): FontFamily = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { weight ->
        Font(
            resource = Res.font.inter_variable,
            weight = weight,
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
        )
    },
)
