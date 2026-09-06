package app.kelkoo.music.ui.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Drive Night amber used by Highway Halo orbital progress. */
val HaloAmber = Color(0xFFE8A838)

/**
 * Amber orbital progress ring around circular album art.
 * Fills from Media3 position/duration; drag on the ring hit-target to seek.
 * Soft breath/pulse while [isPlaying] is true.
 */
@Composable
fun OrbitProgressRing(
    progress: Float,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    ringColor: Color = HaloAmber,
    trackColor: Color = HaloAmber.copy(alpha = 0.22f),
    strokeWidth: Dp = 4.dp,
    /** Outer art diameter; outer hit box is larger (≥48.dp). */
    artSize: Dp = 280.dp,
    onSeekFraction: ((Float) -> Unit)? = null,
    onSeekFinished: ((Float) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    var scrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(clampedProgress) }
    val displayProgress = if (scrubbing) scrubFraction else clampedProgress

    val infinite = rememberInfiniteTransition(label = "haloBreath")
    val breathPulse by infinite.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "haloBreathAlpha",
    )
    val breathAlpha = if (isPlaying && !scrubbing) breathPulse else 1f

    val density = LocalDensity.current
    val strokePx = with(density) { strokeWidth.toPx() }
    val hitPad = 12.dp

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(artSize + hitPad * 2)
            .semantics { contentDescription = "Seek progress" }
            .then(
                if (onSeekFraction != null || onSeekFinished != null) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                scrubbing = true
                                val f = angleToFraction(offset.x, offset.y, size.width.toFloat(), size.height.toFloat())
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
                            onDrag = { change, _ ->
                                change.consume()
                                val f = angleToFraction(
                                    change.position.x,
                                    change.position.y,
                                    size.width.toFloat(),
                                    size.height.toFloat(),
                                )
                                scrubFraction = f
                                onSeekFraction?.invoke(f)
                            },
                        )
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val padPx = hitPad.toPx()
            val diameter = this.size.minDimension - padPx * 2 - strokePx
            val topLeft = Offset(
                (this.size.width - diameter) / 2f,
                (this.size.height - diameter) / 2f,
            )
            val arcSize = Size(diameter, diameter)
            val stroke = Stroke(width = strokePx, cap = StrokeCap.Round)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )

            drawArc(
                color = ringColor.copy(alpha = breathAlpha),
                startAngle = -90f,
                sweepAngle = 360f * displayProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )

            if (scrubbing || displayProgress > 0.001f) {
                val angleRad = Math.toRadians((-90.0 + 360.0 * displayProgress))
                val cx = topLeft.x + diameter / 2f
                val cy = topLeft.y + diameter / 2f
                val r = diameter / 2f
                val thumb = Offset(
                    cx + (cos(angleRad) * r).toFloat(),
                    cy + (sin(angleRad) * r).toFloat(),
                )
                drawCircle(
                    color = ringColor.copy(alpha = if (scrubbing) 1f else breathAlpha * 0.9f),
                    radius = if (scrubbing) strokePx * 1.6f else strokePx * 0.9f,
                    center = thumb,
                )
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(artSize - strokeWidth * 4),
        ) {
            content()
        }
    }
}

private fun angleToFraction(x: Float, y: Float, width: Float, height: Float): Float {
    val cx = width / 2f
    val cy = height / 2f
    val angle = atan2(y - cy, x - cx)
    var degrees = Math.toDegrees(angle.toDouble()).toFloat() + 90f
    if (degrees < 0f) degrees += 360f
    return (degrees / 360f).coerceIn(0f, 1f)
}
