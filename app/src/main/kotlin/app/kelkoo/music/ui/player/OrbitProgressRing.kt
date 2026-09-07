package app.kelkoo.music.ui.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/** Aura gold primary chrome — waveform, dial, selected nav. */
val HaloGold = Color(0xFFD4AF37)

/** @deprecated Use [HaloGold]; kept as alias during Aura Phase 1 migration. */
@Deprecated("Use HaloGold", ReplaceWith("HaloGold"))
val HaloAmber = HaloGold

/**
 * Compact gold progress hairline (legacy mini path). Dial FAB is primary mini.
 */
@Composable
fun WaveProgressHairline(
    progress: Float,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = HaloGold,
    trackColor: Color = HaloGold.copy(alpha = 0.18f),
) {
    val clamped = progress.coerceIn(0f, 1f)
    val infinite = rememberInfiniteTransition(label = "waveHairline")
    val wavePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "waveHairlinePhase",
    )
    val waveActive = isPlaying
    Canvas(modifier = modifier.fillMaxWidth().height(2.dp)) {
        val trackH = size.height
        val cy = size.height / 2f
        val progressX = size.width * clamped
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(0f, 0f),
            size = Size(size.width, trackH),
            cornerRadius = CornerRadius(trackH / 2f, trackH / 2f),
        )
        if (clamped > 0.001f) {
            if (waveActive) {
                val path = Path()
                val amp = trackH * 0.9f
                val steps = (progressX / 2f).toInt().coerceAtLeast(6)
                val wavelength = size.width * 0.2f
                for (i in 0..steps) {
                    val x = progressX * (i.toFloat() / steps)
                    val y = cy + amp * sin((x / wavelength) * 2f * PI.toFloat() + wavePhase)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = activeColor,
                    style = Stroke(width = trackH, cap = StrokeCap.Round),
                )
            } else {
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(0f, 0f),
                    size = Size(progressX, trackH),
                    cornerRadius = CornerRadius(trackH / 2f, trackH / 2f),
                )
            }
        }
    }
}


/**
 * Classic horizontal seek bar with a soft sine wobble on the active fill
 * while [isPlaying] is true. Paused/idle stays calm and flat.
 * Immersive NP gold haptic waveform seek — vertical bars with play-in-wave DNA.
 * Drag or tap to seek; gold on cinematic charcoal.
 */
@Composable
fun WaveSeekBar(
    progress: Float,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = HaloGold,
    trackColor: Color = HaloGold.copy(alpha = 0.22f),
    trackHeight: Dp = 4.dp,
    waveAmplitude: Dp = 3.dp,
    onSeekFraction: ((Float) -> Unit)? = null,
    onSeekFinished: ((Float) -> Unit)? = null,
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    var scrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(clampedProgress) }
    val displayProgress = if (scrubbing) scrubFraction else clampedProgress

    val infinite = rememberInfiniteTransition(label = "waveSeek")
    val wavePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wavePhase",
    )
    val waveActive = isPlaying && !scrubbing

    fun fractionFromX(x: Float, width: Float): Float =
        if (width <= 0f) 0f else (x / width).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp) // ≥48dp total touch with padding from parents; comfy hit height
            .semantics { contentDescription = "Seek progress" }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val f = fractionFromX(offset.x, size.width.toFloat())
                    scrubFraction = f
                    onSeekFraction?.invoke(f)
                    onSeekFinished?.invoke(f)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        scrubbing = true
                        val f = fractionFromX(offset.x, size.width.toFloat())
                        scrubFraction = f
                        onSeekFraction?.invoke(f)
                    },
                    onDragEnd = {
                        onSeekFinished?.invoke(scrubFraction)
                        scrubbing = false
                    },
                    onDragCancel = {
                        scrubbing = false
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val f = fractionFromX(change.position.x, size.width.toFloat())
                        scrubFraction = f
                        onSeekFraction?.invoke(f)
                    },
                )
            },
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(36.dp)) {
            val cy = size.height / 2f
            val progressX = size.width * displayProgress
            val barCount = 48
            val gap = size.width / barCount
            val barW = gap * 0.55f
            val maxH = size.height * 0.92f
            val minH = size.height * 0.16f

            for (i in 0 until barCount) {
                val t = i.toFloat() / (barCount - 1).coerceAtLeast(1)
                // Symmetric haptic envelope peaking near center (immersive NP look)
                val envelope = (sin(t * PI.toFloat()).coerceIn(0.15f, 1f))
                val wobble = if (waveActive) {
                    0.65f + 0.35f * sin(wavePhase + i * 0.55f)
                } else {
                    0.85f
                }
                val h = (minH + (maxH - minH) * envelope * wobble).coerceAtMost(maxH)
                val x = i * gap + (gap - barW) / 2f
                val active = x + barW / 2f <= progressX
                val color = if (active) activeColor else trackColor
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, cy - h / 2f),
                    size = Size(barW, h),
                    cornerRadius = CornerRadius(barW / 2f, barW / 2f),
                )
            }

            // Soft play-center glow marker at scrub position
            if (scrubbing || displayProgress > 0.001f) {
                drawCircle(
                    color = activeColor.copy(alpha = 0.35f),
                    radius = size.height * 0.28f,
                    center = Offset(progressX.coerceIn(0f, size.width), cy),
                )
                drawCircle(
                    color = activeColor,
                    radius = if (scrubbing) 5f else 3.5f,
                    center = Offset(progressX.coerceIn(0f, size.width), cy),
                )
            }
        }
    }
}
