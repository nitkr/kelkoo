package app.kelkoo.music.ui.aura

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kelkoo.music.R
import app.kelkoo.music.ui.theme.DefaultThemeColor
import app.kelkoo.music.ui.theme.bbhBartle
import kotlin.math.PI
import kotlin.math.sin

/** Aura gold — locked primary chrome. */
val AuraGold = DefaultThemeColor
val AuraVoid = Color(0xFF050505)
val AuraGlassFill = Color(0xFF1A1A1A).copy(alpha = 0.55f)
val AuraHairline = AuraGold.copy(alpha = 0.45f)

private val AuraSerif: FontFamily = bbhBartle

/** Lowercase serif wordmark: kelkoo (never classifieds spelling). */
@Composable
fun AuraWordmark(
    modifier: Modifier = Modifier,
    showMusicLabel: Boolean = false,
    fontSize: Int = 22,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "kelkoo",
            color = AuraGold,
            fontFamily = AuraSerif,
            fontWeight = FontWeight.Medium,
            fontSize = fontSize.sp,
            letterSpacing = 1.5.sp,
            maxLines = 1,
        )
        if (showMusicLabel) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = "MUSIC",
                color = AuraGold.copy(alpha = 0.85f),
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                letterSpacing = 4.sp,
            )
        }
    }
}

@Composable
fun AuraSerifTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    fontSize: Int = 28,
    letterSpacing: Float = 0f,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontFamily = AuraSerif,
        fontWeight = FontWeight.Normal,
        fontSize = fontSize.sp,
        letterSpacing = letterSpacing.sp,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun AuraPageDots(
    pageCount: Int,
    current: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (index == current) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == current) AuraGold
                        else Color.White.copy(alpha = 0.22f)
                    )
            )
        }
    }
}

@Composable
fun AuraGlassPillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    leadingIcon: Int? = null,
    /** When true, button wraps content with side margins instead of full-bleed stretch. */
    compact: Boolean = false,
) {
    val shape = RoundedCornerShape(28.dp)
    val bg = if (filled) AuraGold else AuraGlassFill
    val fg = if (filled) Color.Black else AuraGold
    val widthMod = if (compact) Modifier else Modifier.fillMaxWidth()
    Row(
        modifier = modifier
            .then(widthMod)
            .height(if (compact) 48.dp else 50.dp)
            .clip(shape)
            .background(bg)
            .then(
                if (!filled) Modifier.border(1.dp, AuraHairline, shape) else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = if (compact) 28.dp else 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (leadingIcon != null) {
            Icon(
                painter = painterResource(leadingIcon),
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = label,
            color = fg,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            fontFamily = if (!filled) AuraSerif else FontFamily.Default,
            maxLines = 1,
        )
    }
}

@Composable
fun AuraTextLink(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(text = label, color = AuraGold.copy(alpha = 0.9f), fontSize = 14.sp)
    }
}

@Composable
fun AuraGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AuraGlassFill)
            .border(1.dp, AuraHairline.copy(alpha = 0.35f), shape)
            .padding(16.dp),
    ) {
        content()
    }
}

/** Gold waveform illustration for Welcome / Sync / Sonic screens. */
@Composable
fun AuraWaveform(
    modifier: Modifier = Modifier,
    height: Dp = 72.dp,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val midY = size.height / 2f
        val barCount = 48
        val gap = size.width / barCount
        for (i in 0 until barCount) {
            val t = i / (barCount - 1f)
            val envelope = sin(t * PI).toFloat()
            val wobble = (0.35f + 0.65f * sin(t * PI * 6).toFloat().let { kotlin.math.abs(it) })
            val h = size.height * 0.15f + size.height * 0.7f * envelope * wobble
            val x = gap * i + gap / 2f
            drawLine(
                color = AuraGold.copy(alpha = 0.55f + 0.45f * envelope),
                start = Offset(x, midY - h / 2f),
                end = Offset(x, midY + h / 2f),
                strokeWidth = (gap * 0.45f).coerceAtLeast(2f),
                cap = StrokeCap.Round,
            )
        }
    }
}

/** Phone ↔ car sync line-art (Compose approximation). */
@Composable
fun AuraSyncIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(140.dp)) {
        val gold = AuraGold
        val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        // Phone
        val phoneL = size.width * 0.12f
        val phoneT = size.height * 0.18f
        val phoneW = size.width * 0.18f
        val phoneH = size.height * 0.64f
        drawRoundRect(
            color = gold,
            topLeft = Offset(phoneL, phoneT),
            size = androidx.compose.ui.geometry.Size(phoneW, phoneH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
            style = stroke,
        )
        // Car body (simplified)
        val carL = size.width * 0.62f
        val carPath = Path().apply {
            moveTo(carL, size.height * 0.55f)
            lineTo(carL + size.width * 0.08f, size.height * 0.35f)
            lineTo(carL + size.width * 0.22f, size.height * 0.35f)
            lineTo(carL + size.width * 0.28f, size.height * 0.55f)
            close()
        }
        drawPath(carPath, gold, style = stroke)
        drawCircle(gold, radius = 8.dp.toPx(), center = Offset(carL + size.width * 0.08f, size.height * 0.62f), style = stroke)
        drawCircle(gold, radius = 8.dp.toPx(), center = Offset(carL + size.width * 0.22f, size.height * 0.62f), style = stroke)
        // Wave between
        val midY = size.height * 0.5f
        val path = Path()
        val startX = phoneL + phoneW + 8.dp.toPx()
        val endX = carL - 8.dp.toPx()
        path.moveTo(startX, midY)
        var x = startX
        var up = true
        while (x < endX) {
            val next = (x + 14.dp.toPx()).coerceAtMost(endX)
            path.quadraticTo(
                (x + next) / 2f,
                midY + if (up) -18.dp.toPx() else 18.dp.toPx(),
                next,
                midY,
            )
            x = next
            up = !up
        }
        drawPath(path, gold.copy(alpha = 0.85f), style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
    }
}

/** Head + waves sonic profile illustration. */
@Composable
fun AuraSonicIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(180.dp)) {
        val gold = AuraGold
        val cx = size.width * 0.42f
        val cy = size.height * 0.5f
        // Head profile oval
        drawOval(
            color = gold,
            topLeft = Offset(cx - 40.dp.toPx(), cy - 55.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(70.dp.toPx(), 110.dp.toPx()),
            style = Stroke(width = 2.5.dp.toPx()),
        )
        // Concentric arcs (sound)
        for (i in 1..4) {
            val r = 50.dp.toPx() + i * 18.dp.toPx()
            drawArc(
                color = gold.copy(alpha = 0.75f - i * 0.12f),
                startAngle = -55f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(cx - r * 0.2f, cy - r),
                size = androidx.compose.ui.geometry.Size(r * 2f, r * 2f),
                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        // Mini waveform inside head
        val wx = cx - 10.dp.toPx()
        for (i in 0 until 7) {
            val h = (8 + (i % 4) * 6).dp.toPx()
            drawLine(
                gold,
                Offset(wx + i * 5.dp.toPx(), cy - h / 2),
                Offset(wx + i * 5.dp.toPx(), cy + h / 2),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

/** Soft gold aura glow behind content. */
@Composable
fun AuraVoidBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF12100A),
                        AuraVoid,
                        Color(0xFF0A0A0A),
                    )
                )
            )
    )
}

/** Scrub known mood OCR typos from Explore titles. */
fun scrubMoodTitle(raw: String): String {
    var t = raw
    t = t.replace(Regex("(?i)energire"), "Energize")
    t = t.replace(Regex("(?i)\\bexpets\\b"), "experience")
    return t
}
