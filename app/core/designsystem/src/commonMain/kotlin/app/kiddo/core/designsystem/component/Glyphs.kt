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

