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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import app.kelkoo.music.ui.component.CastButton
import app.kelkoo.music.utils.makeTimeString

private val HaloCharcoal = Color(0xFF121212)
private val HaloCharcoalElevated = Color(0xFF1A1A1A)
private val HaloGhost = Color.White.copy(alpha = 0.55f)
private val HaloPlayGlyph = Color(0xFF121212)
private val HeroCorner = RoundedCornerShape(16.dp)

/** Unified stroke weight across transport + secondary rows. */
private val TransportSideIcon = 24.dp
private val TransportSkipIcon = 28.dp
private val TransportPlayIcon = 32.dp
private val SecondaryIcon = 24.dp

/**
 * Highway Halo fullscreen Now Playing — Drive Night charcoal field,
 * full square/rounded-rect album art hero, classic horizontal wave seek bar.
 *
 * Vertical balance: art near top with air; transport lower; secondary row
 * anchored near bottom above safe area / Dial.
 *
 * Dash triad Option A (immersive polish):
 * - Transport: shuffle · prev · play · next · repeat (gold when on)
 * - Footer: Queue | Heart | Add to playlist — Lyrics in ⋮ overflow
 * - Top-left minimize chevron; top-right ⋮ overflow
 * Touch targets ≥48dp; play ≥64dp. Play glyph dark on solid gold.
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
    shuffleModeEnabled: Boolean,
    repeatMode: Int,
    showInlineLyrics: Boolean,
    sleepTimerEnabled: Boolean,
    sleepTimerTimeLeftMs: Long,
    onSeekPreview: (Long) -> Unit = {},
    onSeekCommit: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleLike: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    onAddToPlaylist: () -> Unit = {},
    onMinimize: () -> Unit = {},
    onOpenSleepTimer: () -> Unit,
    onOpenEqualizer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val safeDuration = durationMs.coerceAtLeast(0L)
    val progress = if (safeDuration > 0L) {
        (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    var overflowExpanded by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(HaloCharcoal)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 24.dp),
    ) {
        // Top row — minimize (v) left + ⋮ overflow right
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            IconButton(
                onClick = onMinimize,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.expand_more),
                    contentDescription = stringResource(R.string.close),
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(TransportSideIcon),
                )
            }
            Box {
                IconButton(
                    onClick = { overflowExpanded = true },
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = stringResource(R.string.more_options),
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(TransportSideIcon),
                        )
                        if (sleepTimerEnabled) {
                            Text(
                                text = makeTimeString(sleepTimerTimeLeftMs.coerceAtLeast(0L)),
                                color = HaloGold,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                maxLines = 1,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 2.dp),
                            )
                        }
                    }
                }
                DropdownMenu(
                    expanded = overflowExpanded,
                    onDismissRequest = { overflowExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.lyrics)) },
                        onClick = {
                            overflowExpanded = false
                            onOpenLyrics()
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.lyrics),
                                contentDescription = null,
                                tint = if (showInlineLyrics) HaloGold else Color.Unspecified,
                            )
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (sleepTimerEnabled) {
                                    "${stringResource(R.string.sleep_timer)} · ${makeTimeString(sleepTimerTimeLeftMs.coerceAtLeast(0L))}"
                                } else {
                                    stringResource(R.string.sleep_timer)
                                }
                            )
                        },
                        onClick = {
                            overflowExpanded = false
                            onOpenSleepTimer()
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.sleep_timer),
                                contentDescription = null,
                                tint = if (sleepTimerEnabled) HaloGold else Color.Unspecified,
                            )
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.share)) },
                        onClick = {
                            overflowExpanded = false
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "https://www.youtube.com/watch?v=${mediaMetadata.id}",
                                )
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.share),
                                contentDescription = null,
                            )
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.equalizer)) },
                        onClick = {
                            overflowExpanded = false
                            onOpenEqualizer()
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.equalizer),
                                contentDescription = null,
                            )
                        },
                    )
                    CastButton(
                        tintColor = MaterialTheme.colorScheme.onSurface,
                        asMenuItem = true,
                    )
                }
            }
        }

        // Art near top — capped so controls are not jammed into upper 60%
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.48f, fill = true)
                .padding(top = 8.dp),
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(mediaMetadata.thumbnailUrl)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .clip(HeroCorner)
                    .background(HaloCharcoalElevated),
            )
        }

        Spacer(Modifier.height(16.dp))

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

        Spacer(Modifier.height(14.dp))

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

        // Push transport + secondary toward bottom
        Spacer(Modifier.weight(0.28f))

        // Transport — shuffle · prev · play · next · repeat
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconButton(
                onClick = onToggleShuffle,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    painter = painterResource(
                        if (shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle
                    ),
                    contentDescription = stringResource(R.string.shuffle),
                    tint = if (shuffleModeEnabled) HaloGold else Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(TransportSideIcon),
                )
            }

            IconButton(
                onClick = onSkipPrevious,
                enabled = canSkipPrevious,
                modifier = Modifier.size(52.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.skip_previous),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = if (canSkipPrevious) 0.92f else 0.35f),
                    modifier = Modifier.size(TransportSkipIcon),
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(HaloGold)
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
                    tint = HaloPlayGlyph,
                    modifier = Modifier.size(TransportPlayIcon),
                )
            }

            IconButton(
                onClick = onSkipNext,
                enabled = canSkipNext,
                modifier = Modifier.size(52.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.skip_next),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = if (canSkipNext) 0.92f else 0.35f),
                    modifier = Modifier.size(TransportSkipIcon),
                )
            }

            IconButton(
                onClick = onToggleRepeat,
                modifier = Modifier.size(48.dp),
            ) {
                val repeatOn = repeatMode != Player.REPEAT_MODE_OFF
                Icon(
                    painter = painterResource(
                        when (repeatMode) {
                            Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                            Player.REPEAT_MODE_ALL -> R.drawable.repeat
                            else -> R.drawable.repeat
                        }
                    ),
                    contentDescription = stringResource(R.string.repeat),
                    tint = if (repeatOn) HaloGold else Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(TransportSideIcon),
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Secondary Dash triad — Queue | Heart | Add to playlist (anchored near bottom)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconButton(
                onClick = onOpenQueue,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.queue_music),
                    contentDescription = stringResource(R.string.queue),
                    tint = HaloGhost,
                    modifier = Modifier.size(SecondaryIcon),
                )
            }

            IconButton(
                onClick = onToggleLike,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    painter = painterResource(
                        if (isLiked) R.drawable.favorite else R.drawable.favorite_border
                    ),
                    contentDescription = null,
                    tint = if (isLiked) HaloGold else HaloGhost,
                    modifier = Modifier.size(SecondaryIcon),
                )
            }

            IconButton(
                onClick = onAddToPlaylist,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.playlist_add),
                    contentDescription = stringResource(R.string.add_to_playlist),
                    tint = HaloGhost,
                    modifier = Modifier.size(SecondaryIcon),
                )
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

