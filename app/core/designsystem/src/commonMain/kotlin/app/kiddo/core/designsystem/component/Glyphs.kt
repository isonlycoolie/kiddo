package app.kiddo.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.kiddo.core.designsystem.Kiddo
import app.kiddo.core.model.AiProvider
import app.kiddo.core.resources.generated.BrandLogo
import app.kiddo.core.resources.generated.KiddoGlyph
import app.kiddo.core.resources.generated.LogoFill

// A Hugeicons glyph on its 24 grid, stroked at the design's width.
@Composable
fun KiddoIcon(
    glyph: KiddoGlyph,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    stroke: Float = 1.7f,
    tint: Color = Kiddo.colors.fg,
) {
    val vector = remember(glyph, stroke) { glyphVector(glyph, stroke) }
    Image(
        painter = rememberVectorPainter(vector),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}

private fun glyphVector(glyph: KiddoGlyph, stroke: Float): ImageVector {
    val builder = ImageVector.Builder(glyph.name, 24.dp, 24.dp, 24f, 24f)
    glyph.strokes.forEach { part ->
        builder.addPath(
            pathData = PathParser().parsePathString(part.pathData).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = stroke,
            strokeLineCap = if (part.round) StrokeCap.Round else StrokeCap.Butt,
            strokeLineJoin = if (part.roundJoin) StrokeJoin.Round else StrokeJoin.Miter,
        )
    }
    return builder.build()
}

// A brand logo in its own colours; one-colour marks take the text colour so they read in light and dark.
@Composable
fun BrandLogoImage(logo: BrandLogo, size: Dp, modifier: Modifier = Modifier, label: String? = null) {
    val tint = Kiddo.colors.fg
    val paths = remember(logo) {
        logo.paths.map { part ->
            PathParser().parsePathString(part.pathData).toPath().apply {
                fillType = if (part.evenOdd) PathFillType.EvenOdd else PathFillType.NonZero
            } to part.fill
        }
    }
    val described = if (label != null) Modifier.semantics { contentDescription = label } else Modifier
    Canvas(modifier.then(described).size(size, size * (logo.height / logo.width))) {
        val k = this.size.width / logo.width
        scale(k, k, pivot = Offset.Zero) {
            translate(-logo.x, -logo.y) {
                paths.forEach { (path, fill) ->
                    when (fill) {
                        LogoFill.Tint -> drawPath(path, tint)
                        is LogoFill.Solid -> drawPath(path, Color(fill.argb))
                        is LogoFill.Linear -> drawPath(
                            path,
                            Brush.linearGradient(
                                colorStops = fill.stops.map { (at, argb) -> at to Color(argb) }.toTypedArray(),
                                start = Offset(fill.x1, fill.y1),
                                end = Offset(fill.x2, fill.y2),
                            ),
                        )
                    }
                }
            }
        }
    }
}

// Every logo sits on the same rounded grey tile, so a list of brands lines up like an app list.
@Composable
fun LogoTile(modifier: Modifier = Modifier, size: Dp = 36.dp, content: @Composable () -> Unit) {
    Box(
        modifier.size(size).background(Kiddo.colors.well, RoundedCornerShape(size * 0.28f)),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun ProviderLogo(provider: AiProvider, modifier: Modifier = Modifier, size: Dp = 36.dp) {
    LogoTile(modifier, size) {
        val logo = providerLogo(provider)
        if (logo == null) {
            KiddoIcon(app.kiddo.core.resources.generated.KiddoGlyph.Globe, null, size = size * 0.56f)
        } else {
            BrandLogoImage(logo, size * 0.56f)
        }
    }
}

fun providerLogo(provider: AiProvider): BrandLogo? = when (provider) {
    AiProvider.Anthropic -> BrandLogo.Claude
    AiProvider.OpenAi -> BrandLogo.Openai
    AiProvider.Gemini -> BrandLogo.Gemini
    AiProvider.DeepSeek -> BrandLogo.Deepseek
    AiProvider.Mistral -> BrandLogo.Mistral
    AiProvider.XAi -> BrandLogo.Grok
    AiProvider.Groq -> BrandLogo.Groq
    AiProvider.OpenRouter -> BrandLogo.Openrouter
    AiProvider.Ollama -> BrandLogo.Ollama
    AiProvider.Other -> null
}
