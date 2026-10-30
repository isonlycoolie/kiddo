package app.kiddo.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Black and white first. Colour only signals: ok (done, safe), warn (waiting on you), danger (error, missing,
// destructive). The glow (violet, sky, peach) is for Kiddo's own moments only. Dark is pure black.
@Immutable
data class KiddoColors(
    val bg: Color,
    val fg: Color,
    val muted: Color,
    val faint: Color,
    val line: Color,
    val well: Color,
    val raised: Color,
    val sheet: Color,
    val sheetCard: Color,
    val sheetLine: Color,
    val danger: Color,
    val ok: Color,
    val warn: Color,
    val accent: Color,
    val accentFg: Color,
    val glowA: Color,
    val glowB: Color,
    val glowC: Color,
    val glowAlpha: Float,
    val isDark: Boolean,
)

val LightKiddoColors = KiddoColors(
    bg = Color(0xFFFFFFFF),
    fg = Color(0xFF0D0D0D),
    muted = Color(0xFF8F8F8F),
    faint = Color(0xFFC7C7C7),
    line = Color(0xFFECECEC),
    well = Color(0xFFF4F4F4),
    raised = Color(0xFFE8E8E8),
    sheet = Color(0xFFFFFFFF),
    sheetCard = Color(0xFFF4F4F4),
    sheetLine = Color(0xFFE6E6E6),
    danger = Color(0xFFE0302B),
    ok = Color(0xFF10A37F),
    warn = Color(0xFFD98A00),
    accent = Color(0xFF0D0D0D),
    accentFg = Color(0xFFFFFFFF),
    glowA = Color(0xFF8B6CFF),
    glowB = Color(0xFF3EC6F5),
    glowC = Color(0xFFFFB27A),
    glowAlpha = 0.22f,
    isDark = false,
)

val DarkKiddoColors = KiddoColors(
    bg = Color(0xFF000000),
    fg = Color(0xFFF2F2F2),
    muted = Color(0xFF9B9B9B),
    faint = Color(0xFF555555),
    line = Color(0xFF262626),
    well = Color(0xFF1C1C1C),
    raised = Color(0xFF2E2E2E),
    sheet = Color(0xFF171717),
    sheetCard = Color(0xFF242424),
    sheetLine = Color(0xFF333333),
    danger = Color(0xFFFF5F57),
    ok = Color(0xFF2FD39A),
    warn = Color(0xFFFFB238),
    accent = Color(0xFFF2F2F2),
    accentFg = Color(0xFF000000),
    glowA = Color(0xFF7C5CFF),
    glowB = Color(0xFF2BB5E8),
    glowC = Color(0xFFFF9D6B),
    glowAlpha = 0.3f,
    isDark = true,
)
