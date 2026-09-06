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

/** Drive Night amber used by Highway Halo / full-art NP. */
val HaloAmber = Color(0xFFE8A838)

/**
 * Collapsed Unified Wave Sheet hairline — same amber wave DNA as [WaveSeekBar],
 * but 2dp tall and non-interactive so mini stays tappable for expand/dismiss.
 */
@Composable
fun WaveProgressHairline(
    progress: Float,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = HaloAmber,
    trackColor: Color = HaloAmber.copy(alpha = 0.18f),
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
 * Drag or tap to seek; Drive Night amber on charcoal continuity.
 */
@Composable
fun WaveSeekBar(
    progress: Float,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = HaloAmber,
    trackColor: Color = HaloAmber.copy(alpha = 0.22f),
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
            val trackH = trackHeight.toPx()
            val amp = if (waveActive) waveAmplitude.toPx() else 0f
            val cy = size.height / 2f
            val progressX = size.width * displayProgress

            // Inactive track (flat)
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(0f, cy - trackH / 2f),
                size = Size(size.width, trackH),
                cornerRadius = CornerRadius(trackH / 2f, trackH / 2f),
            )

            if (displayProgress > 0.001f) {
                if (waveActive && amp > 0.5f) {
                    // Wobble wave along the active portion
                    val path = Path()
                    val steps = (progressX / 3f).toInt().coerceAtLeast(8)
                    val wavelength = size.width * 0.18f
                    path.moveTo(0f, cy)
                    for (i in 0..steps) {
                        val x = progressX * (i.toFloat() / steps)
                        val y = cy + amp * sin((x / wavelength) * 2f * PI.toFloat() + wavePhase)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = activeColor,
                        style = Stroke(width = trackH * 1.15f, cap = StrokeCap.Round),
                    )
                } else {
                    // Calm flat active fill
                    drawRoundRect(
                        color = activeColor,
                        topLeft = Offset(0f, cy - trackH / 2f),
                        size = Size(progressX, trackH),
                        cornerRadius = CornerRadius(trackH / 2f, trackH / 2f),
                    )
                }
            }

            // Thumb
            if (scrubbing || displayProgress > 0.001f) {
                drawCircle(
                    color = activeColor,
                    radius = if (scrubbing) trackH * 2.2f else trackH * 1.4f,
                    center = Offset(progressX.coerceIn(0f, size.width), cy),
                )
            }
        }
    }
}
