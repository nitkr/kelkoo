package app.kelkoo.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import app.kelkoo.music.LocalPlayerAwareWindowInsets
import app.kelkoo.music.R
import app.kelkoo.music.constants.AccountNameKey
import app.kelkoo.music.constants.GridItemSize
import app.kelkoo.music.constants.GridItemsSizeKey
import app.kelkoo.music.constants.GridThumbnailHeight
import app.kelkoo.music.constants.InnerTubeCookieKey
import app.kelkoo.music.constants.SmallGridThumbnailHeight
import app.kelkoo.music.ui.aura.AuraGlassPillButton
import app.kelkoo.music.ui.aura.AuraGold
import app.kelkoo.music.ui.component.ChipsRow
import app.kelkoo.music.ui.component.LocalMenuState
import app.kelkoo.music.ui.component.YouTubeGridItem
import app.kelkoo.music.ui.component.shimmer.GridItemPlaceHolder
import app.kelkoo.music.ui.component.shimmer.ShimmerHost
import app.kelkoo.music.ui.menu.YouTubeAlbumMenu
import app.kelkoo.music.ui.menu.YouTubeArtistMenu
import app.kelkoo.music.ui.menu.YouTubePlaylistMenu
import app.kelkoo.music.utils.rememberEnumPreference
import app.kelkoo.music.utils.rememberPreference
import app.kelkoo.music.viewmodels.AccountContentType
import app.kelkoo.music.viewmodels.AccountViewModel
import app.kelkoo.music.viewmodels.HomeViewModel
import coil3.compose.AsyncImage
import com.music.innertube.utils.parseCookieString

/**
 * Aura Profile tab. Root cause of blank/crash on 0.1.8 device builds:
 * - YouTube library calls fail when logged out → states stayed null → infinite shimmer
 * - Dual root TopAppBar + back navigateUp on a main tab could pop/blank the graph
 * Fix: single-root layout, logged-out empty CTA, empty lists on failure, settings gear.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AccountScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    @Suppress("UNUSED_PARAMETER")
    val unusedScroll = scrollBehavior

    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val playlists by viewModel.playlists.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val selectedContentType by viewModel.selectedContentType.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

    val (innerTubeCookie) = rememberPreference(InnerTubeCookieKey, "")
    val (accountNamePref) = rememberPreference(AccountNameKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        innerTubeCookie.isNotEmpty() && "SAPISID" in parseCookieString(innerTubeCookie)
    }
    val homeViewModel: HomeViewModel = hiltViewModel()
    val accountName by homeViewModel.accountName.collectAsState()
    val accountImageUrl by homeViewModel.accountImageUrl.collectAsState()
    val displayName = accountName.ifBlank { accountNamePref }.ifBlank { "Profile" }

    val minCell = if (gridItemSize == GridItemSize.BIG) {
        GridThumbnailHeight + 24.dp
    } else {
        SmallGridThumbnailHeight
    }

    val insets = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    // Shared left gutter with Home / Library (16.dp). Avoid nesting 12+4 and
    // IconButton default min-touch padding — that combo caused the staircase.
    val pagePad = 16.dp

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = minCell.coerceAtLeast(96.dp)),
            contentPadding = PaddingValues(
                start = pagePad,
                end = pagePad,
                top = insets.calculateTopPadding() + 4.dp,
                bottom = insets.calculateBottomPadding() + 16.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Single H1 — no interior wordmark; Settings lives in global top bar + outlined CTA.
                    Text(
                        text = stringResource(R.string.account),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Compact actions share the same left edge as the H1 / avatar / CTAs.
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { navController.navigate("history") },
                            // CenterStart: glyph shares H1/avatar left edge; extra hit area to the right.
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.music_history),
                                contentDescription = stringResource(R.string.history),
                                tint = AuraGold,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { navController.navigate("listen_together") },
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.group_outlined),
                                contentDescription = stringResource(R.string.together),
                                tint = AuraGold,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .border(1.dp, AuraGold.copy(alpha = 0.55f), CircleShape)
                                .background(Color(0xFF1A1A1A)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isLoggedIn && !accountImageUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = accountImageUrl,
                                    contentDescription = displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.person),
                                    contentDescription = null,
                                    tint = AuraGold,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 56.dp),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = if (isLoggedIn) displayName else stringResource(R.string.login),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp,
                            )
                            Text(
                                text = if (isLoggedIn) {
                                    stringResource(R.string.setting_desc_account)
                                } else {
                                    "Sign in to sync playlists, albums, and artists."
                                },
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 12.sp,
                            )
                        }
                    }
                    if (!isLoggedIn) {
                        Spacer(Modifier.height(12.dp))
                        AuraGlassPillButton(
                            label = stringResource(R.string.action_login),
                            onClick = { navController.navigate("login") },
                            filled = true,
                            compact = false,
                        )
                        Spacer(Modifier.height(8.dp))
                        AuraGlassPillButton(
                            label = stringResource(R.string.settings),
                            onClick = { navController.navigate("settings/account") },
                            filled = false,
                            compact = false,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            if (isLoggedIn) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    // Cancel grid pagePad — ChipsRow already leads with 12.dp + system insets.
                    ChipsRow(
                        chips = listOf(
                            AccountContentType.PLAYLISTS to stringResource(R.string.filter_playlists),
                            AccountContentType.ALBUMS to stringResource(R.string.filter_albums),
                            AccountContentType.ARTISTS to stringResource(R.string.filter_artists),
                        ),
                        currentValue = selectedContentType,
                        onValueUpdate = { viewModel.setSelectedContentType(it) },
                        modifier = Modifier.padding(horizontal = -pagePad),
                    )
                }

                when (selectedContentType) {
                    AccountContentType.PLAYLISTS -> {
                        val list = playlists.orEmpty().distinctBy { it.id }
                        items(items = list, key = { it.id }) { item ->
                            YouTubeGridItem(
                                item = item,
                                fillMaxWidth = true,
                                modifier = Modifier.combinedClickable(
                                    onClick = { navController.navigate("online_playlist/${item.id}") },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuState.show {
                                            YouTubePlaylistMenu(
                                                playlist = item,
                                                coroutineScope = coroutineScope,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                ),
                            )
                        }
                        if (isLoading && playlists == null) {
                            items(6) {
                                ShimmerHost { GridItemPlaceHolder(fillMaxWidth = true) }
                            }
                        } else if (!isLoading && list.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                ProfileEmptyHint("No playlists yet")
                            }
                        }
                    }
                    AccountContentType.ALBUMS -> {
                        val list = albums.orEmpty().distinctBy { it.id }
                        items(items = list, key = { it.id }) { item ->
                            YouTubeGridItem(
                                item = item,
                                fillMaxWidth = true,
                                modifier = Modifier.combinedClickable(
                                    onClick = { navController.navigate("album/${item.id}") },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuState.show {
                                            YouTubeAlbumMenu(
                                                albumItem = item,
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                ),
                            )
                        }
                        if (isLoading && albums == null) {
                            items(6) {
                                ShimmerHost { GridItemPlaceHolder(fillMaxWidth = true) }
                            }
                        } else if (!isLoading && list.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                ProfileEmptyHint("No albums yet")
                            }
                        }
                    }
                    AccountContentType.ARTISTS -> {
                        val list = artists.orEmpty().distinctBy { it.id }
                        items(items = list, key = { it.id }) { item ->
                            YouTubeGridItem(
                                item = item,
                                fillMaxWidth = true,
                                modifier = Modifier.combinedClickable(
                                    onClick = { navController.navigate("artist/${item.id}") },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuState.show {
                                            YouTubeArtistMenu(
                                                artist = item,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                ),
                            )
                        }
                        if (isLoading && artists == null) {
                            items(6) {
                                ShimmerHost { GridItemPlaceHolder(fillMaxWidth = true) }
                            }
                        } else if (!isLoading && list.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                ProfileEmptyHint("No artists yet")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileEmptyHint(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF141414))
            .border(1.dp, AuraGold.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = message, color = Color.White.copy(alpha = 0.65f), fontSize = 14.sp)
    }
}
