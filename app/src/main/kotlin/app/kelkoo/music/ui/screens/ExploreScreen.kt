

package app.kelkoo.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import app.kelkoo.music.LocalPlayerAwareWindowInsets
import app.kelkoo.music.LocalPlayerConnection
import app.kelkoo.music.R
import app.kelkoo.music.constants.ListItemHeight
import app.kelkoo.music.models.toMediaMetadata
import app.kelkoo.music.playback.queues.YouTubeQueue
import app.kelkoo.music.ui.aura.AuraGold
import app.kelkoo.music.ui.aura.AuraTextLink
import app.kelkoo.music.ui.aura.scrubMoodTitle
import app.kelkoo.music.ui.component.LocalMenuState
import app.kelkoo.music.ui.component.NavigationTitle
import app.kelkoo.music.ui.component.YouTubeGridItem
import app.kelkoo.music.ui.component.YouTubeListItem
import app.kelkoo.music.ui.component.shimmer.GridItemPlaceHolder
import app.kelkoo.music.ui.component.shimmer.ShimmerHost
import app.kelkoo.music.ui.component.shimmer.TextPlaceholder
import app.kelkoo.music.ui.menu.YouTubeAlbumMenu
import app.kelkoo.music.ui.menu.YouTubeSongMenu
import app.kelkoo.music.ui.utils.SnapLayoutInfoProvider
import app.kelkoo.music.viewmodels.ChartsViewModel
import app.kelkoo.music.viewmodels.ExploreViewModel

// Match Home / Library / Account (16.dp).
private val ExplorePagePad = 16.dp
private val MetaGrey = Color(0xFFA0A0A0)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val chartsError by chartsViewModel.error.collectAsState()

    val coroutineScope = rememberCoroutineScope()
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

    val chartSections = remember(chartsPage) {
        chartsPage?.sections?.filter { it.title != "Top music videos" }.orEmpty()
    }
    val chartSongCount = remember(chartSections) {
        chartSections.sumOf { section ->
            section.items.filterIsInstance<SongItem>().distinctBy { it.id }.size
        }
    }
    val moodAndGenres = remember(moodItems, genreItems, explorePage) {
        val fromShelves = (moodItems + genreItems).distinctBy { it.title + it.endpoint.browseId }
        fromShelves.ifEmpty { explorePage?.moodAndGenres.orEmpty() }
    }
    val newReleases = explorePage?.newReleaseAlbums?.distinctBy { it.id }.orEmpty()

    val isLoading = (isChartsLoading || isExploreLoading) &&
        chartSongCount == 0 && moodAndGenres.isEmpty() && newReleases.isEmpty()

    fun refreshAll() {
        chartsViewModel.loadCharts()
        exploreViewModel.refresh()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.verticalScroll(scrollState),
        ) {
            Spacer(
                Modifier.height(
                    LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateTopPadding() + 4.dp,
                ),
            )

            // Keep E2 search + settings chrome; content restored to pre-E2 density below.
            ExploreSearchBar(
                onSearch = {
                    navController.navigate(Screens.Search.route) {
                        launchSingleTop = true
                    }
                },
                onSettings = { navController.navigate("settings") },
                modifier = Modifier.padding(horizontal = ExplorePagePad),
            )

            Spacer(Modifier.height(12.dp))

            if (isLoading) {
                ShimmerHost {
                    TextPlaceholder(
                        height = 36.dp,
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(0.5f),
                    )
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val horizontalLazyGridItemWidthFactor =
                            if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
                        val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor

                        LazyHorizontalGrid(
                            rows = GridCells.Fixed(4),
                            contentPadding = PaddingValues(start = 4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ListItemHeight * 4),
                        ) {
                            items(4) {
                                Row(
                                    modifier = Modifier
                                        .width(horizontalLazyGridItemWidth)
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(ListItemHeight - 16.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.onSurface),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(
                                        modifier = Modifier.fillMaxHeight(),
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .height(16.dp)
                                                .width(120.dp)
                                                .background(MaterialTheme.colorScheme.onSurface),
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .height(12.dp)
                                                .width(80.dp)
                                                .background(MaterialTheme.colorScheme.onSurface),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    TextPlaceholder(
                        height = 36.dp,
                        modifier = Modifier
                            .padding(vertical = 12.dp, horizontal = 12.dp)
                            .width(250.dp),
                    )
                    Row {
                        repeat(2) {
                            GridItemPlaceHolder()
                        }
                    }

                    TextPlaceholder(
                        height = 36.dp,
                        modifier = Modifier
                            .padding(vertical = 12.dp, horizontal = 12.dp)
                            .width(250.dp),
                    )
                    repeat(4) {
                        Row {
                            repeat(2) {
                                TextPlaceholder(
                                    height = MoodAndGenresButtonHeight,
                                    modifier = Modifier
                                        .padding(horizontal = 6.dp)
                                        .width(200.dp),
                                )
                            }
                        }
                    }
                }
            } else {
                if (chartSections.isEmpty() || chartSongCount == 0) {
                    NavigationTitle(
                        title = stringResource(R.string.charts),
                        onClick = { navController.navigate("charts_screen") },
                    )
                    ExploreEmptyShelf(
                        onRefresh = ::refreshAll,
                        errorMessage = chartsError,
                        modifier = Modifier.padding(horizontal = ExplorePagePad),
                    )
                } else {
                    chartSections.forEach { section ->
                        val songs = section.items.filterIsInstance<SongItem>().distinctBy { it.id }
                        if (songs.isEmpty()) return@forEach

                        NavigationTitle(
                            title = when (section.title) {
                                "Trending" -> stringResource(R.string.trending)
                                else -> section.title.ifEmpty { stringResource(R.string.kelkoo_chart) }
                            },
                            onClick = { navController.navigate("charts_screen") },
                        )
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val horizontalLazyGridItemWidthFactor =
                                if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
                            val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor

                            val lazyGridState = rememberLazyGridState()
                            val snapLayoutInfoProvider = remember(lazyGridState) {
                                SnapLayoutInfoProvider(
                                    lazyGridState = lazyGridState,
                                    positionInLayout = { layoutSize, itemSize ->
                                        (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                                    },
                                )
                            }

                            LazyHorizontalGrid(
                                state = lazyGridState,
                                rows = GridCells.Fixed(4),
                                flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider),
                                contentPadding = WindowInsets.systemBars
                                    .only(WindowInsetsSides.Horizontal)
                                    .asPaddingValues(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(ListItemHeight * 4),
                            ) {
                                itemsIndexed(
                                    items = songs,
                                    key = { _, it -> it.id },
                                ) { _, song ->
                                    YouTubeListItem(
                                        item = song,
                                        isActive = song.id == mediaMetadata?.id,
                                        isPlaying = isPlaying,
                                        isSwipeable = false,
                                        color = Color.Transparent,
                                        shape = RectangleShape,
                                        trailingContent = {
                                            IconButton(
                                                onClick = {
                                                    menuState.show {
                                                        YouTubeSongMenu(
                                                            song = song,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.more_vert),
                                                    contentDescription = null,
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .width(horizontalLazyGridItemWidth)
                                            .combinedClickable(
                                                onClick = {
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
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        YouTubeSongMenu(
                                                            song = song,
                                                            navController = navController,
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
                }

                if (newReleases.isEmpty()) {
                    NavigationTitle(
                        title = stringResource(R.string.new_release_albums),
                        onClick = { navController.navigate("new_release") },
                    )
                    ExploreEmptyShelf(
                        onRefresh = ::refreshAll,
                        modifier = Modifier.padding(horizontal = ExplorePagePad),
                    )
                } else {
                    NavigationTitle(
                        title = stringResource(R.string.new_release_albums),
                        onClick = { navController.navigate("new_release") },
                    )
                    LazyRow(
                        contentPadding = WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal)
                            .asPaddingValues(),
                    ) {
                        items(
                            items = newReleases,
                            key = { it.id },
                        ) { album ->
                            YouTubeGridItem(
                                item = album,
                                isActive = mediaMetadata?.album?.id == album.id,
                                isPlaying = isPlaying,
                                coroutineScope = coroutineScope,
                                modifier = Modifier
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("album/${album.id}")
                                        },
                                        onLongClick = {
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
                                    .animateItem(),
                            )
                        }
                    }
                }

                chartsPage?.sections?.find { it.title == "Top music videos" }?.let { topVideosSection ->
                    val videos = topVideosSection.items.filterIsInstance<SongItem>().distinctBy { it.id }
                    if (videos.isNotEmpty()) {
                        NavigationTitle(
                            title = stringResource(R.string.top_music_videos),
                        )
                        LazyRow(
                            contentPadding = WindowInsets.systemBars
                                .only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                        ) {
                            items(
                                items = videos,
                                key = { it.id },
                            ) { video ->
                                YouTubeGridItem(
                                    item = video,
                                    isActive = video.id == mediaMetadata?.id,
                                    isPlaying = isPlaying,
                                    coroutineScope = coroutineScope,
                                    modifier = Modifier
                                        .combinedClickable(
                                            onClick = {
                                                if (video.id == mediaMetadata?.id) {
                                                    playerConnection.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(
                                                        YouTubeQueue(
                                                            endpoint = WatchEndpoint(videoId = video.id),
                                                            preloadItem = video.toMediaMetadata(),
                                                        ),
                                                    )
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    YouTubeSongMenu(
                                                        song = video,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        )
                                        .animateItem(),
                                )
                            }
                        }
                    }
                }

                if (moodAndGenres.isEmpty()) {
                    NavigationTitle(
                        title = stringResource(R.string.mood_and_genres),
                        onClick = { navController.navigate("mood_and_genres") },
                    )
                    ExploreEmptyShelf(
                        onRefresh = ::refreshAll,
                        modifier = Modifier.padding(horizontal = ExplorePagePad),
                    )
                } else {
                    NavigationTitle(
                        title = stringResource(R.string.mood_and_genres),
                        onClick = { navController.navigate("mood_and_genres") },
                    )
                    LazyHorizontalGrid(
                        rows = GridCells.Fixed(4),
                        contentPadding = PaddingValues(6.dp),
                        modifier = Modifier.height((MoodAndGenresButtonHeight + 12.dp) * 4 + 12.dp),
                    ) {
                        items(moodAndGenres) {
                            MoodAndGenresButton(
                                title = scrubMoodTitle(it.title),
                                onClick = {
                                    navController.navigate(
                                        "youtube_browse/${it.endpoint.browseId}?params=${it.endpoint.params}",
                                    )
                                },
                                modifier = Modifier
                                    .padding(6.dp)
                                    .width(180.dp),
                            )
                        }
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
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
private fun ExploreEmptyShelf(
    onRefresh: () -> Unit,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = stringResource(R.string.nothing_here_yet),
            color = MetaGrey,
            fontSize = 14.sp,
        )
        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                color = MetaGrey.copy(alpha = 0.85f),
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        AuraTextLink(
            label = stringResource(R.string.refresh),
            onClick = onRefresh,
            flushStart = true,
        )
    }
}
