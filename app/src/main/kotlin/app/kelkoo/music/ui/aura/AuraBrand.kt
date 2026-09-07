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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.StrokeJoin
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

/** Multi-word onboarding titles — restrained sans, never ultra-wide display face. */
@Composable
fun AuraReadableTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    fontSize: Int = 26,
    fontWeight: FontWeight = FontWeight.SemiBold,
    textAlign: TextAlign = TextAlign.Center,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontFamily = FontFamily.Default,
        fontWeight = fontWeight,
        fontSize = fontSize.sp,
        letterSpacing = 0.sp,
        textAlign = textAlign,
        lineHeight = (fontSize + 6).sp,
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
    trailingIcon: Int? = null,
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
            .height(if (compact) 50.dp else 52.dp)
            .clip(shape)
            .background(bg)
            .then(
                if (!filled) Modifier.border(1.dp, AuraHairline, shape) else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = if (compact) 28.dp else 22.dp),
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
            // UI CTAs always use readable sans — never ultra-wide display face.
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            fontFamily = FontFamily.Default,
            maxLines = 1,
        )
        if (trailingIcon != null) {
            Spacer(Modifier.width(6.dp))
            Icon(
                painter = painterResource(trailingIcon),
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(18.dp),
            )
        }
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
            .background(Color(0xFF1A1A1A).copy(alpha = 0.62f))
            .border(1.dp, AuraHairline.copy(alpha = 0.42f), shape)
            .padding(16.dp),
    ) {
        content()
    }
}


/**
 * Shared P1/P2 rhythm: hero cluster vertically centered in the space above a
 * fixed bottom CTA slot (thumb-consistent with Welcome Get Started).
 * Does not alter scroll-heavy P3–P5 pages.
 */
@Composable
fun AuraBottomAnchoredHero(
    modifier: Modifier = Modifier,
    cta: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                content()
            }
        }
        cta()
        Spacer(Modifier.height(4.dp))
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

/** Phone ↔ car sync — refined modern outline icons, Aura gold stroke. */
@Composable
fun AuraSyncIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(156.dp)) {
        val gold = AuraGold
        val strokeThin = Stroke(
            width = 2.4.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        val stroke = Stroke(
            width = 3.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )

        // —— Phone (left): modern rounded device with speaker + screen ——
        val phoneW = size.width * 0.16f
        val phoneH = size.height * 0.72f
        val phoneL = size.width * 0.10f
        val phoneT = (size.height - phoneH) / 2f
        val phoneRadius = 14.dp.toPx()
        drawRoundRect(
            color = gold,
            topLeft = Offset(phoneL, phoneT),
            size = androidx.compose.ui.geometry.Size(phoneW, phoneH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(phoneRadius),
            style = stroke,
        )
        // Speaker pill
        val speakerW = phoneW * 0.34f
        drawRoundRect(
            color = gold.copy(alpha = 0.85f),
            topLeft = Offset(phoneL + (phoneW - speakerW) / 2f, phoneT + phoneH * 0.08f),
            size = androidx.compose.ui.geometry.Size(speakerW, 4.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round),
        )
        // Inner screen
        val inset = 7.dp.toPx()
        drawRoundRect(
            color = gold.copy(alpha = 0.55f),
            topLeft = Offset(phoneL + inset, phoneT + phoneH * 0.16f),
            size = androidx.compose.ui.geometry.Size(phoneW - inset * 2f, phoneH * 0.68f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
            style = strokeThin,
        )

        // —— Car (right): aerodynamic side profile, soft curves matching phone ——
        val carL = size.width * 0.56f
        val carR = size.width * 0.92f
        val carW = carR - carL
        val groundY = size.height * 0.76f
        val sillY = size.height * 0.56f
        val roofY = size.height * 0.24f
        val hoodY = size.height * 0.46f
        val wheelR = 9.5.dp.toPx()
        val wellR = wheelR + 2.5.dp.toPx()
        val frontWx = carL + carW * 0.27f
        val rearWx = carL + carW * 0.73f
        val bodyBottom = groundY - wheelR * 0.15f

        // Continuous coupe silhouette with cubic sweeps + integrated wheel wells
        val carBody = Path().apply {
            // Front bumper → soft nose into hood
            moveTo(carL + carW * 0.04f, sillY)
            cubicTo(
                carL - carW * 0.02f, sillY + 2.dp.toPx(),
                carL + carW * 0.01f, hoodY + 6.dp.toPx(),
                carL + carW * 0.16f, hoodY,
            )
            // Hood sweep into windshield
            cubicTo(
                carL + carW * 0.24f, hoodY - 4.dp.toPx(),
                carL + carW * 0.28f, roofY + 20.dp.toPx(),
                carL + carW * 0.36f, roofY + 2.dp.toPx(),
            )
            // Soft roof arc
            cubicTo(
                carL + carW * 0.44f, roofY - 5.dp.toPx(),
                carL + carW * 0.60f, roofY - 5.dp.toPx(),
                carL + carW * 0.70f, roofY + 4.dp.toPx(),
            )
            // Rear glass → deck → soft rear bumper
            cubicTo(
                carL + carW * 0.80f, roofY + 18.dp.toPx(),
                carL + carW * 0.86f, hoodY,
                carL + carW * 0.96f, sillY,
            )
            cubicTo(
                carR + carW * 0.03f, sillY + 4.dp.toPx(),
                carR, bodyBottom - 8.dp.toPx(),
                carR - carW * 0.05f, bodyBottom,
            )
            // Underbody with rear wheel well (arc into chassis)
            lineTo(rearWx + wellR, bodyBottom)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    rearWx - wellR,
                    bodyBottom - wellR * 1.55f,
                    rearWx + wellR,
                    bodyBottom + wellR * 0.45f,
                ),
                startAngleDegrees = 12f,
                sweepAngleDegrees = -204f,
                forceMoveTo = false,
            )
            // Sill between wells
            lineTo(frontWx + wellR, bodyBottom)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    frontWx - wellR,
                    bodyBottom - wellR * 1.55f,
                    frontWx + wellR,
                    bodyBottom + wellR * 0.45f,
                ),
                startAngleDegrees = 12f,
                sweepAngleDegrees = -204f,
                forceMoveTo = false,
            )
            // Front underbody closes into bumper
            lineTo(carL + carW * 0.08f, bodyBottom)
            cubicTo(
                carL + carW * 0.02f, bodyBottom,
                carL - carW * 0.01f, sillY + 10.dp.toPx(),
                carL + carW * 0.04f, sillY,
            )
            close()
        }
        drawPath(carBody, gold, style = stroke)

        // Cabin glass — soft parallelogram with rounded joins
        val win = Path().apply {
            moveTo(carL + carW * 0.34f, sillY - 2.dp.toPx())
            cubicTo(
                carL + carW * 0.36f, roofY + 18.dp.toPx(),
                carL + carW * 0.40f, roofY + 8.dp.toPx(),
                carL + carW * 0.46f, roofY + 8.dp.toPx(),
            )
            cubicTo(
                carL + carW * 0.58f, roofY + 6.dp.toPx(),
                carL + carW * 0.64f, roofY + 8.dp.toPx(),
                carL + carW * 0.70f, roofY + 14.dp.toPx(),
            )
            cubicTo(
                carL + carW * 0.74f, roofY + 22.dp.toPx(),
                carL + carW * 0.76f, sillY - 6.dp.toPx(),
                carL + carW * 0.72f, sillY - 2.dp.toPx(),
            )
            close()
        }
        drawPath(win, gold.copy(alpha = 0.7f), style = strokeThin)

        // Wheels sit inside wells (same stroke family as phone)
        val wheelY = groundY
        drawCircle(gold, radius = wheelR, center = Offset(frontWx, wheelY), style = stroke)
        drawCircle(
            gold.copy(alpha = 0.75f),
            radius = wheelR * 0.38f,
            center = Offset(frontWx, wheelY),
            style = strokeThin,
        )
        drawCircle(gold, radius = wheelR, center = Offset(rearWx, wheelY), style = stroke)
        drawCircle(
            gold.copy(alpha = 0.75f),
            radius = wheelR * 0.38f,
            center = Offset(rearWx, wheelY),
            style = strokeThin,
        )

        // —— Center: smooth dual sync arrows (cycle) ——
        val cx = size.width * 0.455f
        val cy = size.height * 0.42f
        val r = 18.dp.toPx()
        // Top arc → rightward
        drawArc(
            color = gold,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(cx - r, cy - r),
            size = androidx.compose.ui.geometry.Size(r * 2f, r * 2f),
            style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round),
        )
        // Bottom arc → leftward
        drawArc(
            color = gold,
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(cx - r, cy - r),
            size = androidx.compose.ui.geometry.Size(r * 2f, r * 2f),
            style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round),
        )
        // Arrowheads
        val tipTop = Offset(cx + r * 0.72f, cy - r * 0.55f)
        drawLine(gold, tipTop, Offset(tipTop.x - 7.dp.toPx(), tipTop.y - 5.dp.toPx()), strokeWidth = 2.8.dp.toPx(), cap = StrokeCap.Round)
        drawLine(gold, tipTop, Offset(tipTop.x - 7.dp.toPx(), tipTop.y + 5.dp.toPx()), strokeWidth = 2.8.dp.toPx(), cap = StrokeCap.Round)
        val tipBot = Offset(cx - r * 0.72f, cy + r * 0.55f)
        drawLine(gold, tipBot, Offset(tipBot.x + 7.dp.toPx(), tipBot.y - 5.dp.toPx()), strokeWidth = 2.8.dp.toPx(), cap = StrokeCap.Round)
        drawLine(gold, tipBot, Offset(tipBot.x + 7.dp.toPx(), tipBot.y + 5.dp.toPx()), strokeWidth = 2.8.dp.toPx(), cap = StrokeCap.Round)
    }
}

/** Sonic profile — circular sound burst (waveform + tight arcs), not biometric. */
@Composable
fun AuraSonicIllustration(
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
) {
    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val gold = AuraGold
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f
        // Match mood-chip border weight (~1.dp UI → ~2.8–3.dp canvas stroke)
        val strokeBold = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        val strokeMid = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)

        // Perfect circle container — music/speaker, not fingerprint oval
        val radius = minOf(size.height * 0.28f, 48.dp.toPx())
        drawCircle(
            color = gold,
            radius = radius,
            center = Offset(cx, cy),
            style = strokeBold,
        )

        // Bold waveform bars filling the circle
        val barCount = 9
        val barGap = radius * 1.35f / barCount
        val barLeft = cx - (barCount - 1) * barGap / 2f
        val heights = floatArrayOf(0.28f, 0.48f, 0.72f, 0.92f, 1.0f, 0.92f, 0.72f, 0.48f, 0.28f)
        for (i in 0 until barCount) {
            val h = radius * 1.15f * heights[i]
            val x = barLeft + i * barGap
            drawLine(
                color = gold,
                start = Offset(x, cy - h / 2f),
                end = Offset(x, cy + h / 2f),
                strokeWidth = 3.2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        // Tight radiating arcs — one burst hugging the circle (not distant rings)
        for (i in 1..3) {
            val r = radius + 8.dp.toPx() + (i - 1) * 10.dp.toPx()
            val alpha = 0.85f - (i - 1) * 0.18f
            val sweep = 56f - (i - 1) * 6f
            drawArc(
                color = gold.copy(alpha = alpha),
                startAngle = -sweep / 2f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = androidx.compose.ui.geometry.Size(r * 2f, r * 2f),
                style = strokeMid,
            )
            drawArc(
                color = gold.copy(alpha = alpha),
                startAngle = 180f - sweep / 2f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = androidx.compose.ui.geometry.Size(r * 2f, r * 2f),
                style = strokeMid,
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
