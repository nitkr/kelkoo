package app.kelkoo.music.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import app.kelkoo.music.LocalListenTogetherManager
import app.kelkoo.music.LocalPlayerConnection
import app.kelkoo.music.R
import app.kelkoo.music.constants.MiniPlayerHeight
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import kotlin.math.cos
import kotlin.math.sin

/**
 * Aura Dial FAB — mini-player above glass bottom bar (not a tab).
 * Fans stay fully visible + hittable above glass nav; swipe-up to Immersive NP
 * only from this Dial cluster. Fold-safe end bias for SM-F966B-class widths.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialFabMiniPlayer(
    progressState: ProgressState,
    modifier: Modifier = Modifier,
    onOpenNowPlaying: () -> Unit = {},
    onDismiss: (() -> Unit)? = null,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val librarySong by playerConnection.currentSong.collectAsState(initial = null)
    val isLiked = librarySong?.song?.liked == true

    val castHandler = remember(playerConnection) {
        try {
            playerConnection.service.castConnectionHandler
        } catch (_: Exception) {
            null
        }
    }
    val isCasting by castHandler?.isCasting?.collectAsState() ?: remember { mutableStateOf(false) }
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val effectiveIsPlaying = if (isCasting) castIsPlaying else isPlaying

    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isGuestPlaybackRestricted } ?: false

    var dialExpanded by remember { mutableStateOf(false) }
    val fanProgress by animateFloatAsState(
        targetValue = if (dialExpanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "dialFan",
    )

    @Suppress("UNUSED_VARIABLE")
    val dismissHook = onDismiss

    val configuration = LocalConfiguration.current
    // SM-F966B / Fold unfolded ≈ 600–900+ dp wide — keep Dial on outer trailing pane past hinge.
    val isFoldExpanded = configuration.screenWidthDp >= 600
    val dialEndPadding = when {
        configuration.screenWidthDp >= 840 -> 52.dp
        isFoldExpanded -> 32.dp
        else -> 16.dp
    }
    val dialBottomPadding = if (isFoldExpanded) 10.dp else 6.dp
    val clusterSize = if (dialExpanded) 168.dp else 148.dp
    val dialSize = 76.dp
    val playHitSize = 56.dp

    val density = LocalDensity.current
    var swipeAccum by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(MiniPlayerHeight)
            .semantics { contentDescription = "Dial mini player" },
    ) {
        if (dialExpanded) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(if (isFoldExpanded) 0.55f else 0.5f)
                    .height(MiniPlayerHeight)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { dialExpanded = false },
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = dialEndPadding, bottom = dialBottomPadding)
                .size(clusterSize)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { swipeAccum = 0f },
                        onVerticalDrag = { _, dragAmount ->
                            swipeAccum += dragAmount
                        },
                        onDragCancel = { swipeAccum = 0f },
                        onDragEnd = {
                            val threshold = with(density) { 28.dp.toPx() }
                            if (swipeAccum < -threshold) {
                                onOpenNowPlaying()
                            }
                            swipeAccum = 0f
                        },
                    )
                },
        ) {
            if (fanProgress > 0.02f) {
                DialFanButton(
                    icon = R.drawable.skip_previous,
                    enabled = canSkipPrevious && !isListenTogetherGuest,
                    angleDeg = -52f,
                    progress = fanProgress,
                    radiusDp = 62f,
                    onClick = {
                        if (!isListenTogetherGuest) {
                            playerConnection.player.seekToPreviousMediaItem()
                        }
                    },
                )
                DialFanButton(
                    icon = if (isLiked) R.drawable.favorite else R.drawable.favorite_border,
                    enabled = !isListenTogetherGuest,
                    angleDeg = 0f,
                    progress = fanProgress,
                    radiusDp = 62f,
                    tint = if (isLiked) HaloGold else Color.White,
                    onClick = {
                        if (!isListenTogetherGuest) {
                            playerConnection.toggleLike()
                        }
                    },
                )
                DialFanButton(
                    icon = R.drawable.skip_next,
                    enabled = canSkipNext && !isListenTogetherGuest,
                    angleDeg = 52f,
                    progress = fanProgress,
                    radiusDp = 62f,
                    onClick = {
                        if (!isListenTogetherGuest) {
                            playerConnection.player.seekToNext()
                        }
                    },
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(dialSize)
                    .shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        ambientColor = HaloGold.copy(alpha = 0.4f),
                        spotColor = HaloGold.copy(alpha = 0.5f),
                    )
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            if (!dialExpanded) {
                                dialExpanded = true
                            } else {
                                onOpenNowPlaying()
                            }
                        },
                        onLongClick = { onOpenNowPlaying() },
                    ),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = size.minDimension * 0.1f
                    val radius = size.minDimension / 2f - stroke / 2f
                    drawCircle(
                        brush = Brush.sweepGradient(
                            listOf(
                                Color(0xFFB8860B),
                                HaloGold,
                                Color(0xFFF5E6A3),
                                HaloGold,
                                Color(0xFFB8860B),
                            ),
                        ),
                        radius = radius,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                    drawCircle(
                        color = Color(0xFFF5E6A3).copy(alpha = 0.5f),
                        radius = radius - stroke * 0.55f,
                        style = Stroke(width = stroke * 0.25f),
                    )
                    val progress = progressState.progress.coerceIn(0f, 1f)
                    val beadAngle = (-90f + progress * 360f) * (Math.PI.toFloat() / 180f)
                    val cx = center.x + radius * cos(beadAngle)
                    val cy = center.y + radius * sin(beadAngle)
                    drawCircle(color = HaloGold, radius = stroke * 0.55f, center = Offset(cx, cy))
                    drawCircle(color = Color(0xFFF5E6A3), radius = stroke * 0.28f, center = Offset(cx, cy))
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(playHitSize)
                        .clip(CircleShape)
                        .background(Color(0xFF141414))
                        .border(1.dp, HaloGold.copy(alpha = 0.4f), CircleShape),
                ) {
                    AnimatedContent(
                        targetState = dialExpanded,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "dialCenter",
                    ) { expanded ->
                        if (expanded) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(HaloGold, CircleShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) {
                                        if (isListenTogetherGuest) {
                                            playerConnection.toggleMute()
                                            return@clickable
                                        }
                                        if (isCasting) {
                                            if (castIsPlaying) castHandler?.pause() else castHandler?.play()
                                        } else if (playbackState == Player.STATE_ENDED) {
                                            playerConnection.player.seekTo(0, 0)
                                            playerConnection.player.playWhenReady = true
                                        } else {
                                            playerConnection.togglePlayPause()
                                        }
                                    },
                            ) {
                                Icon(
                                    painter = painterResource(
                                        when {
                                            playbackState == Player.STATE_ENDED -> R.drawable.replay
                                            effectiveIsPlaying -> R.drawable.pause
                                            else -> R.drawable.play
                                        },
                                    ),
                                    contentDescription = "Play pause",
                                    tint = Color(0xFF0F0F0F),
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        } else {
                            mediaMetadata?.let { metadata ->
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(metadata.thumbnailUrl)
                                        .build(),
                                    contentDescription = metadata.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialFanButton(
    icon: Int,
    enabled: Boolean,
    angleDeg: Float,
    progress: Float,
    onClick: () -> Unit,
    tint: Color = Color.White,
    radiusDp: Float = 54f,
) {
    val rad = Math.toRadians(angleDeg.toDouble() - 90.0)
    val radiusPx = radiusDp * progress
    val x = (radiusPx * cos(rad)).dp
    val y = (radiusPx * sin(rad)).dp
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .offset(x = x, y = y)
            .size(44.dp)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(listOf(Color(0xFF2A2A2A), Color(0xFF141414))),
            )
            .border(1.dp, HaloGold.copy(alpha = 0.55f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = if (enabled) tint else tint.copy(alpha = 0.35f),
            modifier = Modifier.size(22.dp),
        )
    }
}
