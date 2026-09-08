

package app.kelkoo.music.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.innertube.pages.MoodAndGenres
import app.kelkoo.music.LocalPlayerAwareWindowInsets
import app.kelkoo.music.LocalPlayerConnection
import app.kelkoo.music.R
import app.kelkoo.music.models.toMediaMetadata
import app.kelkoo.music.playback.queues.YouTubeQueue
import app.kelkoo.music.ui.aura.AuraGold
import app.kelkoo.music.ui.aura.scrubMoodTitle
import app.kelkoo.music.ui.component.LocalMenuState
import app.kelkoo.music.ui.component.shimmer.ShimmerHost
import app.kelkoo.music.ui.menu.YouTubeAlbumMenu
import app.kelkoo.music.ui.menu.YouTubeSongMenu
import app.kelkoo.music.ui.theme.bbhBartle
import app.kelkoo.music.viewmodels.ChartsViewModel
import app.kelkoo.music.viewmodels.ExploreViewModel
import kotlinx.coroutines.delay

private val ExplorePagePad = 18.dp
private val ExploreShelfGap = 30.dp
private val ChartRowHeight = 68.dp
private val ChartRowGap = 8.dp
private val MoodCardWidth = 160.dp
private val MoodCardHeight = 200.dp
private val NewReleaseArt = 120.dp
private val GenreTileMinHeight = 72.dp
private val MetaGrey = Color(0xFFA0A0A0)

private val GenreIcons = listOf(
    R.drawable.headset_applemusic,
    R.drawable.star,
    R.drawable.ic_heart,
    R.drawable.music_note,
    R.drawable.album,
    R.drawable.favorite,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExploreScreen(
    navController: NavController,
    exploreViewModel: ExploreViewModel = hiltViewModel(),
    chartsViewModel: ChartsViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val explorePage by exploreViewModel.explorePage.collectAsState()
    val moodItems by exploreViewModel.moodItems.collectAsState()
    val genreItems by exploreViewModel.genreItems.collectAsState()
    val isExploreLoading by exploreViewModel.isLoading.collectAsState()
    val chartsPage by chartsViewModel.chartsPage.collectAsState()
    val isChartsLoading by chartsViewModel.isLoading.collectAsState()

    val scrollState = rememberScrollState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop by backStackEntry?.savedStateHandle
        ?.getStateFlow("scrollToTop", false)?.collectAsState() ?: return

    LaunchedEffect(Unit) {
        if (chartsPage == null) {
            chartsViewModel.loadCharts()
        }
    }

    LaunchedEffect(scrollToTop) {
        if (scrollToTop) {
            scrollState.animateScrollTo(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    val chartSongs = remember(chartsPage) {
        chartsPage?.sections
            ?.firstOrNull { it.title != "Top music videos" }
            ?.items
            ?.filterIsInstance<SongItem>()
            ?.distinctBy { it.id }
            ?.take(8)
            .orEmpty()
    }

    val moods = moodItems.ifEmpty { explorePage?.moodAndGenres.orEmpty() }
    val genres = genreItems.ifEmpty { explorePage?.moodAndGenres.orEmpty() }
    val newReleases = explorePage?.newReleaseAlbums?.distinctBy { it.id }.orEmpty()

    val isLoading = (isChartsLoading || isExploreLoading) &&
        chartSongs.isEmpty() && moods.isEmpty() && newReleases.isEmpty()

    fun refreshAll() {
        chartsViewModel.loadCharts()
        exploreViewModel.refresh()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = ExplorePagePad),
        ) {
            Spacer(
                Modifier.height(
                    LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateTopPadding() + 4.dp,
                ),
            )

            // Search + Settings only (no logo on Explore). Mock search field; chrome Settings via route.
            ExploreSearchBar(
                onSearch = {
                    navController.navigate(Screens.Search.route) {
                        launchSingleTop = true
                    }
                },
                onSettings = {
                    navController.navigate("settings")
                },
            )

            Spacer(Modifier.height(20.dp))

            if (isLoading) {
                ExploreClearShelvesSkeleton()
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val dualRail = maxWidth >= 600.dp
                    Column(modifier = Modifier.fillMaxWidth()) {
                    if (dualRail) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                ChartsShelf(
                                    songs = chartSongs,
                                    isPlaying = isPlaying,
                                    activeId = mediaMetadata?.id,
                                    onSeeAll = { navController.navigate("charts_screen") },
                                    onRefresh = ::refreshAll,
                                    onPlay = { song ->
                                        if (song.id == mediaMetadata?.id) {
                                            playerConnection.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                YouTubeQueue(
                                                    endpoint = WatchEndpoint(videoId = song.id),
                                                    preloadItem = song.toMediaMetadata(),
                                                ),
                                            )
                                        }
                                    },
                                    onMenu = { song ->
                                        menuState.show {
                                            YouTubeSongMenu(
                                                song = song,
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                    onLongPress = { song ->
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuState.show {
                                            YouTubeSongMenu(
                                                song = song,
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                MoodsShelf(
                                    moods = moods,
                                    onClick = { mood ->
                                        navController.navigate(
                                            "youtube_browse/${mood.endpoint.browseId}?params=${mood.endpoint.params}",
                                        )
                                    },
                                    onSeeAll = { navController.navigate("mood_and_genres") },
                                    onRefresh = ::refreshAll,
                                )
                            }
                        }
                        Spacer(Modifier.height(ExploreShelfGap))
                    } else {
                        ChartsShelf(
                            songs = chartSongs,
                            isPlaying = isPlaying,
                            activeId = mediaMetadata?.id,
                            onSeeAll = { navController.navigate("charts_screen") },
                            onRefresh = ::refreshAll,
                            onPlay = { song ->
                                if (song.id == mediaMetadata?.id) {
                                    playerConnection.togglePlayPause()
                                } else {
                                    playerConnection.playQueue(
                                        YouTubeQueue(
                                            endpoint = WatchEndpoint(videoId = song.id),
                                            preloadItem = song.toMediaMetadata(),
                                        ),
                                    )
                                }
                            },
                            onMenu = { song ->
                                menuState.show {
                                    YouTubeSongMenu(
                                        song = song,
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            },
                            onLongPress = { song ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    YouTubeSongMenu(
                                        song = song,
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            },
                        )
                        Spacer(Modifier.height(ExploreShelfGap))

                        MoodsShelf(
                            moods = moods,
                            onClick = { mood ->
                                navController.navigate(
                                    "youtube_browse/${mood.endpoint.browseId}?params=${mood.endpoint.params}",
                                )
                            },
                            onSeeAll = { navController.navigate("mood_and_genres") },
                            onRefresh = ::refreshAll,
                        )
                        Spacer(Modifier.height(ExploreShelfGap))
                    }

                    NewReleasesShelf(
                        albums = newReleases,
                        isPlaying = isPlaying,
                        activeAlbumId = mediaMetadata?.album?.id,
                        onSeeAll = { navController.navigate("new_release") },
                        onRefresh = ::refreshAll,
                        onClick = { album -> navController.navigate("album/${album.id}") },
                        onLongPress = { album ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                YouTubeAlbumMenu(
                                    albumItem = album,
                                    navController = navController,
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
                    )
                    Spacer(Modifier.height(ExploreShelfGap))

                    GenresShelf(
                        genres = genres,
                        columns = if (dualRail) 3 else 2,
                        onClick = { genre ->
                            navController.navigate(
                                "youtube_browse/${genre.endpoint.browseId}?params=${genre.endpoint.params}",
                            )
                        },
                        onRefresh = ::refreshAll,
                    )
                    }
                }
            }

            Spacer(
                Modifier.height(
                    LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding() + 16.dp,
                ),
            )
        }
    }
}

@Composable
private fun ExploreSearchBar(
    onSearch: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1A1A1A).copy(alpha = 0.85f))
                .border(1.dp, AuraGold.copy(alpha = 0.22f), RoundedCornerShape(24.dp))
                .clickable(onClick = onSearch)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.search),
                contentDescription = null,
                tint = MetaGrey,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.search_music),
                color = MetaGrey,
                fontSize = 14.sp,
            )
        }
        IconButton(onClick = onSettings) {
            Icon(
                painter = painterResource(R.drawable.settings),
                contentDescription = stringResource(R.string.settings),
                tint = AuraGold,
            )
        }
    }
}

@Composable
private fun ExploreShelfHeader(
    title: String,
    onSeeAll: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = AuraGold,
            fontFamily = bbhBartle,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (onSeeAll != null) {
            Text(
                text = stringResource(R.string.see_all),
                color = AuraGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onSeeAll)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun ExploreEmptyShelf(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = stringResource(R.string.nothing_here_yet),
            color = MetaGrey,
            fontSize = 14.sp,
        )
        TextButton(onClick = onRefresh) {
            Text(
                text = stringResource(R.string.refresh),
                color = AuraGold,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun ShelfEnter(content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(16)
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(280),
        label = "shelfAlpha",
    )
    val rise by animateFloatAsState(
        targetValue = if (visible) 0f else 8f,
        animationSpec = tween(280),
        label = "shelfRise",
    )
    Box(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha
            translationY = rise
        },
    ) {
        content()
    }
}

@Composable
private fun ChartsShelf(
    songs: List<SongItem>,
    isPlaying: Boolean,
    activeId: String?,
    onSeeAll: () -> Unit,
    onRefresh: () -> Unit,
    onPlay: (SongItem) -> Unit,
    onMenu: (SongItem) -> Unit,
    onLongPress: (SongItem) -> Unit,
) {
    ShelfEnter {
        Column(modifier = Modifier.fillMaxWidth()) {
            ExploreShelfHeader(
                title = stringResource(R.string.charts),
                onSeeAll = onSeeAll,
            )
            if (songs.isEmpty()) {
                ExploreEmptyShelf(onRefresh = onRefresh)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(ChartRowGap)) {
                    songs.forEachIndexed { index, song ->
                        ChartRankRow(
                            rank = index + 1,
                            song = song,
                            isActive = song.id == activeId,
                            isPlaying = isPlaying && song.id == activeId,
                            onClick = { onPlay(song) },
                            onMenu = { onMenu(song) },
                            onLongPress = { onLongPress(song) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartRankRow(
    rank: Int,
    song: SongItem,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMenu: () -> Unit,
    onLongPress: () -> Unit,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ChartRowHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF121212).copy(alpha = 0.55f))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress,
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rank.toString(),
            color = AuraGold,
            fontFamily = bbhBartle,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(28.dp),
        )
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(song.thumbnail)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = if (isActive) AuraGold else Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artists.joinToString { it.name },
                color = MetaGrey,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onMenu) {
            Icon(
                painter = painterResource(R.drawable.more_vert),
                contentDescription = null,
                tint = MetaGrey,
            )
        }
    }
}

@Composable
private fun MoodsShelf(
    moods: List<MoodAndGenres.Item>,
    onClick: (MoodAndGenres.Item) -> Unit,
    onSeeAll: () -> Unit,
    onRefresh: () -> Unit,
) {
    ShelfEnter {
        Column(modifier = Modifier.fillMaxWidth()) {
            ExploreShelfHeader(
                title = stringResource(R.string.moods),
                onSeeAll = onSeeAll,
            )
            if (moods.isEmpty()) {
                ExploreEmptyShelf(onRefresh = onRefresh)
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 4.dp),
                ) {
                    items(moods.take(16), key = { it.title + it.endpoint.browseId }) { mood ->
                        MoodCinematicCard(
                            title = scrubMoodTitle(mood.title),
                            stripeColor = mood.stripeColor,
                            onClick = { onClick(mood) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodCinematicCard(
    title: String,
    stripeColor: Long,
    onClick: () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(120),
        label = "moodPress",
    )
    val base = Color(stripeColor.toInt() or 0xFF000000.toInt())
    Box(
        modifier = Modifier
            .size(width = MoodCardWidth, height = MoodCardHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        try {
                            tryAwaitRelease()
                        } finally {
                            pressed = false
                        }
                    },
                    onTap = { onClick() },
                )
            },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            base.copy(alpha = 0.85f),
                            Color(0xFF1A2228),
                            Color(0xFF0A0A0A),
                        ),
                    ),
                ),
        )
        Text(
            text = title,
            color = Color.White,
            fontFamily = bbhBartle,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
        )
    }
}

@Composable
private fun NewReleasesShelf(
    albums: List<AlbumItem>,
    isPlaying: Boolean,
    activeAlbumId: String?,
    onSeeAll: () -> Unit,
    onRefresh: () -> Unit,
    onClick: (AlbumItem) -> Unit,
    onLongPress: (AlbumItem) -> Unit,
) {
    val context = LocalContext.current
    ShelfEnter {
        Column(modifier = Modifier.fillMaxWidth()) {
            ExploreShelfHeader(
                title = stringResource(R.string.new_releases),
                onSeeAll = onSeeAll,
            )
            if (albums.isEmpty()) {
                ExploreEmptyShelf(onRefresh = onRefresh)
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 4.dp),
                ) {
                    items(albums, key = { it.id }) { album ->
                        Column(
                            modifier = Modifier
                                .width(NewReleaseArt)
                                .combinedClickable(
                                    onClick = { onClick(album) },
                                    onLongClick = { onLongPress(album) },
                                ),
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(album.thumbnail)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = album.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(NewReleaseArt)
                                    .clip(RoundedCornerShape(10.dp))
                                    .then(
                                        if (album.id == activeAlbumId && isPlaying) {
                                            Modifier.border(1.dp, AuraGold, RoundedCornerShape(10.dp))
                                        } else {
                                            Modifier
                                        },
                                    ),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = album.title,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = album.artists?.joinToString { it.name }.orEmpty(),
                                color = MetaGrey,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GenresShelf(
    genres: List<MoodAndGenres.Item>,
    columns: Int,
    onClick: (MoodAndGenres.Item) -> Unit,
    onRefresh: () -> Unit,
) {
    ShelfEnter {
        Column(modifier = Modifier.fillMaxWidth()) {
            ExploreShelfHeader(title = stringResource(R.string.genres))
            if (genres.isEmpty()) {
                ExploreEmptyShelf(onRefresh = onRefresh)
            } else {
                val rows = genres.chunked(columns)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            rowItems.forEachIndexed { indexInRow, genre ->
                                val globalIndex = genres.indexOf(genre).coerceAtLeast(0)
                                GenreTile(
                                    title = scrubMoodTitle(genre.title),
                                    iconRes = GenreIcons[globalIndex % GenreIcons.size],
                                    onClick = { onClick(genre) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            repeat(columns - rowItems.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GenreTile(
    title: String,
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(GenreTileMinHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF141414))
            .border(1.dp, AuraGold.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = AuraGold,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ExploreClearShelvesSkeleton() {
    ShimmerHost {
        // Spec: 4 chart rows + 3 mood card placeholders (gold shimmer ≤10% alpha)
        Text(
            text = stringResource(R.string.charts),
            color = AuraGold.copy(alpha = 0.35f),
            fontFamily = bbhBartle,
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ChartRowHeight)
                    .padding(bottom = ChartRowGap)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AuraGold.copy(alpha = 0.08f)),
            )
        }
        Spacer(Modifier.height(ExploreShelfGap))
        Text(
            text = stringResource(R.string.moods),
            color = AuraGold.copy(alpha = 0.35f),
            fontFamily = bbhBartle,
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(width = MoodCardWidth, height = MoodCardHeight)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AuraGold.copy(alpha = 0.08f)),
                )
            }
        }
    }
}
