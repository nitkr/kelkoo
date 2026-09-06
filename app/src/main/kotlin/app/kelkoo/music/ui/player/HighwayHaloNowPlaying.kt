package app.kelkoo.music.ui.player

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import app.kelkoo.music.R
import app.kelkoo.music.models.MediaMetadata
import app.kelkoo.music.utils.makeTimeString

private val HaloCharcoal = Color(0xFF121212)
private val HaloCharcoalElevated = Color(0xFF1A1A1A)
private val HaloGhost = Color.White.copy(alpha = 0.55f)
private val HeroCorner = RoundedCornerShape(16.dp)

/**
 * Highway Halo fullscreen Now Playing — Drive Night charcoal field,
 * full square/rounded-rect album art hero, classic horizontal wave seek bar.
 * Orbital ring retired. MediaSession / Android Auto NP remain system; phone UI only.
 */
@Composable
fun HighwayHaloNowPlaying(
    mediaMetadata: MediaMetadata,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    playbackState: Int,
    isLiked: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    onSeekPreview: (Long) -> Unit = {},
    onSeekCommit: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleLike: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val safeDuration = durationMs.coerceAtLeast(0L)
    val progress = if (safeDuration > 0L) {
        (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(HaloCharcoal)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Spacer(Modifier.weight(0.2f))

        // Full album art hero — square rounded-rect, fills hero (not a disc)
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(mediaMetadata.thumbnailUrl)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(HeroCorner)
                .background(HaloCharcoalElevated),
        )

        Spacer(Modifier.height(28.dp))

        Text(
            text = mediaMetadata.title,
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .basicMarquee(iterations = 1, initialDelayMillis = 2500, velocity = 28.dp),
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = mediaMetadata.artists.joinToString { it.name },
            color = Color.White.copy(alpha = 0.65f),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(20.dp))

        // Classic horizontal seek + wave while playing
        WaveSeekBar(
            progress = progress,
            isPlaying = isPlaying,
            onSeekFraction = { fraction ->
                if (safeDuration > 0L) {
                    onSeekPreview((fraction * safeDuration).toLong())
                }
            },
            onSeekFinished = { fraction ->
                if (safeDuration > 0L) {
                    onSeekCommit((fraction * safeDuration).toLong())
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
        ) {
            Text(
                text = makeTimeString(positionMs.coerceAtLeast(0L)),
                color = HaloGhost,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
            Text(
                text = if (safeDuration > 0L) makeTimeString(safeDuration) else "",
                color = HaloGhost,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
        }

        Spacer(Modifier.height(24.dp))

        // Transport — generous spacing, ≥48dp hit targets
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(36.dp),
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            IconButton(
                onClick = onSkipPrevious,
                enabled = canSkipPrevious,
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.skip_previous),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = if (canSkipPrevious) 0.92f else 0.35f),
                    modifier = Modifier.size(36.dp),
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(HaloAmber.copy(alpha = 0.16f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onTogglePlayPause,
                    ),
            ) {
                Icon(
                    painter = painterResource(
                        when {
                            playbackState == Player.STATE_ENDED -> R.drawable.replay
                            isPlaying -> R.drawable.pause
                            else -> R.drawable.play
                        }
                    ),
                    contentDescription = null,
                    tint = HaloAmber,
                    modifier = Modifier.size(36.dp),
                )
            }

            IconButton(
                onClick = onSkipNext,
                enabled = canSkipNext,
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.skip_next),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = if (canSkipNext) 0.92f else 0.35f),
                    modifier = Modifier.size(36.dp),
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // Quiet ghost actions: like / queue / share
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(
                onClick = onToggleLike,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    painter = painterResource(
                        if (isLiked) R.drawable.favorite else R.drawable.favorite_border
                    ),
                    contentDescription = null,
                    tint = if (isLiked) HaloAmber else HaloGhost,
                    modifier = Modifier.size(22.dp),
                )
            }
            IconButton(
                onClick = onOpenQueue,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.queue_music),
                    contentDescription = null,
                    tint = HaloGhost,
                    modifier = Modifier.size(22.dp),
                )
            }
            IconButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "https://www.youtube.com/watch?v=${mediaMetadata.id}",
                        )
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.share),
                    contentDescription = null,
                    tint = HaloGhost,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Spacer(Modifier.weight(0.45f))
        Spacer(Modifier.height(8.dp))
    }
}
