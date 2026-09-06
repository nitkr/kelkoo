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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
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
import app.kelkoo.music.ui.component.LocalMenuState
import app.kelkoo.music.ui.component.NavigationTitle
import app.kelkoo.music.ui.component.PlaylistGridItem
import app.kelkoo.music.ui.component.SongGridItem
import app.kelkoo.music.ui.menu.PlaylistMenu
import app.kelkoo.music.ui.menu.SongMenu
import app.kelkoo.music.ui.theme.DefaultThemeColor
import app.kelkoo.music.viewmodels.HomeViewModel
import java.util.Calendar

/**
 * Hybrid UX Home — resume canvas only: greeting, Continue hero, Recents, Pinned.
 * Discovery shelves live on Explore ([HomeScreen]).
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
                        // Full NP is the player bottom sheet; expand via playing/resume.
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

            item(key = "recents_title") {
                NavigationTitle(
                    title = stringResource(R.string.recents),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            item(key = "recents_row") {
                if (recentSongs.isEmpty()) {
                    Text(
                        text = stringResource(R.string.quick_picks_empty),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                } else {
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
                                                    )
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

            item(key = "pinned_title") {
                NavigationTitle(
                    title = stringResource(R.string.pinned),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            item(key = "pinned_row") {
                if (pinnedPlaylists.isEmpty()) {
                    Text(
                        text = stringResource(R.string.pin_from_library),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                } else {
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

            item(key = "bottom_spacer") {
                Spacer(Modifier.height(32.dp))
            }
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
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
                        contentColor = androidx.compose.ui.graphics.Color.Black,
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
