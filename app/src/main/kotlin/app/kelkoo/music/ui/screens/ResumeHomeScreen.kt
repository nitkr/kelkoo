package app.kelkoo.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.innertube.models.YTItem
import app.kelkoo.music.LocalDatabase
import app.kelkoo.music.LocalPlayerAwareWindowInsets
import app.kelkoo.music.LocalPlayerConnection
import app.kelkoo.music.R
import app.kelkoo.music.constants.PlaylistSortType
import app.kelkoo.music.db.entities.Song
import app.kelkoo.music.extensions.toMediaItem
import app.kelkoo.music.models.toMediaMetadata
import app.kelkoo.music.playback.queues.ListQueue
import app.kelkoo.music.playback.queues.YouTubeQueue
import app.kelkoo.music.ui.aura.AuraGold
import app.kelkoo.music.ui.aura.AuraWarmStartDisc
import app.kelkoo.music.ui.component.LocalMenuState
import app.kelkoo.music.ui.component.NavigationTitle
import app.kelkoo.music.ui.component.PlaylistGridItem
import app.kelkoo.music.ui.component.SongGridItem
import app.kelkoo.music.ui.component.YouTubeGridItem
import app.kelkoo.music.ui.menu.PlaylistMenu
import app.kelkoo.music.ui.menu.SongMenu
import app.kelkoo.music.ui.menu.YouTubeAlbumMenu
import app.kelkoo.music.ui.menu.YouTubeArtistMenu
import app.kelkoo.music.ui.menu.YouTubePlaylistMenu
import app.kelkoo.music.ui.menu.YouTubeSongMenu
import app.kelkoo.music.ui.theme.DefaultThemeColor
import app.kelkoo.music.viewmodels.HomeViewModel
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope

/**
 * Hybrid UX Home — resume canvas + warm-start discovery shelves for empty libraries.
 * Full Explore shelves still live on Explore ([HomeScreen]).
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ResumeHomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()

    val events by database.events().collectAsState(initial = emptyList())
    val continueSong = remember(events) { events.firstOrNull()?.song }
    val recentSongs = remember(events) {
        events.map { it.song }.distinctBy { it.id }.take(20)
    }

    val playlists by database.playlists(PlaylistSortType.CREATE_DATE, descending = true)
        .collectAsState(initial = emptyList())
    val pinnedPlaylists = remember(playlists) {
        playlists.filter { it.playlist.isPinned }
    }

    val warmStartTrending by viewModel.warmStartTrending.collectAsState()
    val warmStartMadeForYou by viewModel.warmStartMadeForYou.collectAsState()
    val warmStartMoodTitle by viewModel.warmStartMoodTitle.collectAsState()
    val warmStartMoodItems by viewModel.warmStartMoodItems.collectAsState()

    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> R.string.good_morning
            in 12..16 -> R.string.good_afternoon
            else -> R.string.good_evening
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            listState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    val madeForYouTitle = stringResource(R.string.made_for_you)
    val trendingNowTitle = stringResource(R.string.trending_now)
    val quickPicksTitle = stringResource(R.string.quick_picks)
    val madeForYouLabel = if (warmStartMadeForYou.isNotEmpty()) madeForYouTitle else null
    val trendingLabel = when {
        warmStartTrending.isEmpty() -> null
        warmStartTrending.any { it is AlbumItem || it is PlaylistItem } -> trendingNowTitle
        else -> quickPicksTitle
    }
    val moodLabel = warmStartMoodTitle.ifBlank { null }

    PullToRefreshBox(
        state = pullRefreshState,
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
            )
        },
    ) {
        LazyColumn(
            state = listState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "greeting") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    ImageMark()
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(greeting),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    )
                }
            }

            val hasContinue = continueSong != null
            val hasRecents = recentSongs.isNotEmpty()
            val hasPinned = pinnedPlaylists.isNotEmpty()
            val isResumeEmpty = !hasContinue && !hasRecents && !hasPinned

            if (isResumeEmpty) {
                item(key = "warm_start_cta") {
                    WarmStartDiscoverCta(
                        onDiscover = { navController.navigate(Screens.Explore.route) },
                    )
                }
            }

            // Time-based mood row (under greeting / CTA) — Explore search path.
            warmStartYtCarousel(
                keyPrefix = "mood",
                title = moodLabel,
                items = warmStartMoodItems,
                navController = navController,
                isPlaying = isPlaying,
                mediaId = mediaMetadata?.id,
                albumId = mediaMetadata?.album?.id,
                scope = scope,
                haptic = haptic,
                menuState = menuState,
                playerPlay = { item ->
                    playerConnection.playQueue(
                        YouTubeQueue(
                            item.endpoint ?: WatchEndpoint(videoId = item.id),
                            item.toMediaMetadata(),
                        ),
                    )
                },
                togglePlayPause = playerConnection::togglePlayPause,
            )

            if (hasContinue) {
                item(key = "continue_hero") {
                    ContinueHero(
                        song = continueSong,
                        isPlaying = isPlaying && continueSong?.id == mediaMetadata?.id,
                        onPlay = { song ->
                            if (song.id == mediaMetadata?.id) {
                                playerConnection.togglePlayPause()
                            } else {
                                playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                            }
                        },
                        onOpenPlayer = {
                            continueSong?.let { song ->
                                if (song.id != mediaMetadata?.id) {
                                    playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                                }
                            }
                        },
                        onLongPress = { song ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                SongMenu(
                                    originalSong = song,
                                    navController = navController,
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
                    )
                }
            }

            if (hasRecents) {
                item(key = "recents_title") {
                    NavigationTitle(
                        title = stringResource(R.string.recents),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                item(key = "recents_row") {
                    LazyRow(
                        contentPadding = WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal)
                            .asPaddingValues()
                            .let { PaddingValues(horizontal = 12.dp) },
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(recentSongs, key = { it.id }) { song ->
                            SongGridItem(
                                song = song,
                                modifier = Modifier
                                    .combinedClickable(
                                        onClick = {
                                            if (song.id == mediaMetadata?.id) {
                                                playerConnection.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = song.title,
                                                        items = recentSongs.map { it.toMediaItem() },
                                                        startIndex = recentSongs.indexOfFirst { it.id == song.id }.coerceAtLeast(0),
                                                    ),
                                                )
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    ),
                                isActive = song.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                            )
                        }
                    }
                }
            }

            if (hasPinned) {
                item(key = "pinned_title") {
                    NavigationTitle(
                        title = stringResource(R.string.pinned),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                item(key = "pinned_row") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(pinnedPlaylists, key = { it.id }) { playlist ->
                            PlaylistGridItem(
                                playlist = playlist,
                                modifier = Modifier.combinedClickable(
                                    onClick = {
                                        navController.navigate("local_playlist/${playlist.id}")
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuState.show {
                                            PlaylistMenu(
                                                playlist = playlist,
                                                coroutineScope = scope,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                ),
                            )
                        }
                    }
                }
            }

            // Made for You — onboarding languages (skip if none).
            warmStartYtCarousel(
                keyPrefix = "made_for_you",
                title = madeForYouLabel,
                items = warmStartMadeForYou,
                navController = navController,
                isPlaying = isPlaying,
                mediaId = mediaMetadata?.id,
                albumId = mediaMetadata?.album?.id,
                scope = scope,
                haptic = haptic,
                menuState = menuState,
                playerPlay = { item ->
                    playerConnection.playQueue(
                        YouTubeQueue(
                            item.endpoint ?: WatchEndpoint(videoId = item.id),
                            item.toMediaMetadata(),
                        ),
                    )
                },
                togglePlayPause = playerConnection::togglePlayPause,
            )

            // Trending Now / Quick Picks — same Explore home/explore path.
            warmStartYtCarousel(
                keyPrefix = "trending",
                title = trendingLabel,
                items = warmStartTrending,
                navController = navController,
                isPlaying = isPlaying,
                mediaId = mediaMetadata?.id,
                albumId = mediaMetadata?.album?.id,
                scope = scope,
                haptic = haptic,
                menuState = menuState,
                playerPlay = { item ->
                    playerConnection.playQueue(
                        YouTubeQueue(
                            item.endpoint ?: WatchEndpoint(videoId = item.id),
                            item.toMediaMetadata(),
                        ),
                    )
                },
                togglePlayPause = playerConnection::togglePlayPause,
            )

            item(key = "bottom_spacer") {
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.warmStartYtCarousel(
    keyPrefix: String,
    title: String?,
    items: List<YTItem>,
    navController: NavController,
    isPlaying: Boolean,
    mediaId: String?,
    albumId: String?,
    scope: CoroutineScope,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    menuState: app.kelkoo.music.ui.component.MenuState,
    playerPlay: (SongItem) -> Unit,
    togglePlayPause: () -> Unit,
) {
    if (title == null || items.isEmpty()) return

    item(key = "${keyPrefix}_title") {
        NavigationTitle(
            title = title,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
    item(key = "${keyPrefix}_row") {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(items.distinctBy { it.id }, key = { it.id }) { ytItem ->
                YouTubeGridItem(
                    item = ytItem,
                    isActive = ytItem.id in listOfNotNull(mediaId, albumId),
                    isPlaying = isPlaying,
                    coroutineScope = scope,
                    thumbnailRatio = 1f,
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            when (ytItem) {
                                is SongItem -> {
                                    if (ytItem.id == mediaId) togglePlayPause()
                                    else playerPlay(ytItem)
                                }
                                is AlbumItem -> navController.navigate("album/${ytItem.id}")
                                is ArtistItem -> navController.navigate("artist/${ytItem.id}")
                                is PlaylistItem -> {
                                    val playlistId = ytItem.id.removePrefix("VL")
                                    when (playlistId) {
                                        "LM" -> navController.navigate("auto_playlist/liked")
                                        "SE" -> navController.navigate("auto_playlist/downloaded")
                                        else -> navController.navigate("online_playlist/$playlistId")
                                    }
                                }
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                when (ytItem) {
                                    is SongItem -> YouTubeSongMenu(
                                        song = ytItem,
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                    is AlbumItem -> YouTubeAlbumMenu(
                                        albumItem = ytItem,
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                    is ArtistItem -> YouTubeArtistMenu(
                                        artist = ytItem,
                                        onDismiss = menuState::dismiss,
                                    )
                                    is PlaylistItem -> YouTubePlaylistMenu(
                                        playlist = ytItem,
                                        coroutineScope = scope,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            }
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun WarmStartDiscoverCta(onDiscover: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AuraWarmStartDisc(size = 96.dp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.home_warm_start_subtitle),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onDiscover,
            colors = ButtonDefaults.buttonColors(
                containerColor = AuraGold,
                contentColor = Color.Black,
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                painter = painterResource(R.drawable.explore_filled),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.discover_new_music),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
private fun ImageMark() {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(DefaultThemeColor.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.music_note),
            contentDescription = null,
            tint = DefaultThemeColor,
            modifier = Modifier.size(16.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContinueHero(
    song: Song?,
    isPlaying: Boolean,
    onPlay: (Song) -> Unit,
    onOpenPlayer: () -> Unit,
    onLongPress: (Song) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.continue_listening),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        if (song == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.nothing_playing_pick),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                )
            }
            return
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .combinedClickable(
                    onClick = onOpenPlayer,
                    onLongClick = { onLongPress(song) },
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = song.artists.joinToString { it.name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onPlay(song) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DefaultThemeColor,
                        contentColor = Color.Black,
                    ),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(
                        painter = painterResource(
                            if (isPlaying) R.drawable.pause else R.drawable.play
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                }
            }
        }
    }
}
