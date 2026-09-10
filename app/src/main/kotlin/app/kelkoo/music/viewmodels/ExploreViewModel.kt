

package app.kelkoo.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.innertube.YouTube
import com.music.innertube.models.filterExplicit
import com.music.innertube.pages.ExplorePage
import com.music.innertube.pages.MoodAndGenres
import app.kelkoo.music.constants.HideExplicitKey
import app.kelkoo.music.db.MusicDatabase
import app.kelkoo.music.utils.ContentLanguageSupport
import app.kelkoo.music.utils.dataStore
import app.kelkoo.music.utils.get
import app.kelkoo.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    val database: MusicDatabase,
) : ViewModel() {
    val explorePage = MutableStateFlow<ExplorePage?>(null)

    private val _moodItems = MutableStateFlow<List<MoodAndGenres.Item>>(emptyList())
    val moodItems = _moodItems.asStateFlow()

    private val _genreItems = MutableStateFlow<List<MoodAndGenres.Item>>(emptyList())
    val genreItems = _genreItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private suspend fun load() {
        _isLoading.value = true
        ContentLanguageSupport.applyToYouTube(context.dataStore)
        YouTube
            .explore()
            .onSuccess { page ->
                val artists: MutableMap<Int, String> = mutableMapOf()
                val favouriteArtists: MutableMap<Int, String> = mutableMapOf()
                database.allArtistsByPlayTime().first().let { list ->
                    var favIndex = 0
                    for ((artistsIndex, artist) in list.withIndex()) {
                        artists[artistsIndex] = artist.id
                        if (artist.artist.bookmarkedAt != null) {
                            favouriteArtists[favIndex] = artist.id
                            favIndex++
                        }
                    }
                }
                explorePage.value =
                    page.copy(
                        newReleaseAlbums =
                        page.newReleaseAlbums
                            .sortedBy { album ->
                                val artistIds = album.artists.orEmpty().mapNotNull { it.id }
                                val firstArtistKey =
                                    artistIds.firstNotNullOfOrNull { artistId ->
                                        if (artistId in favouriteArtists.values) {
                                            favouriteArtists.entries.firstOrNull { it.value == artistId }?.key
                                        } else {
                                            artists.entries.firstOrNull { it.value == artistId }?.key
                                        }
                                    } ?: Int.MAX_VALUE
                                firstArtistKey
                            }.filterExplicit(context.dataStore.get(HideExplicitKey, false)),
                    )
                splitMoodsAndGenres(page.moodAndGenres)
            }.onFailure {
                reportException(it)
            }

        // Prefer sectioned moods/genres page when available (E2 Moods vs Genres shelves).
        // Do not wipe explore-derived shelves with empty halves; keep explore fallback for Moods.
        YouTube.moodAndGenres()
            .onSuccess { sections ->
                val moods = sections.firstOrNull {
                    it.title.contains("mood", ignoreCase = true)
                }?.items.orEmpty()
                val genres = sections.firstOrNull {
                    it.title.contains("genre", ignoreCase = true)
                }?.items.orEmpty()
                if (moods.isNotEmpty()) {
                    _moodItems.value = moods
                }
                if (genres.isNotEmpty()) {
                    _genreItems.value = genres
                }
                // If Moods still empty after split, fall back to explore moodAndGenres carousel.
                if (_moodItems.value.isEmpty()) {
                    val exploreItems = explorePage.value?.moodAndGenres.orEmpty()
                    if (exploreItems.isNotEmpty()) {
                        _moodItems.value = exploreItems
                    }
                }
            }
            .onFailure {
                // Non-fatal — explore carousel already seeded shelves.
                reportException(it)
            }
        _isLoading.value = false
    }

    private fun splitMoodsAndGenres(items: List<MoodAndGenres.Item>) {
        if (_moodItems.value.isNotEmpty() || _genreItems.value.isNotEmpty()) return
        val genreKeywords = listOf(
            "hip-hop", "hip hop", "pop", "rock", "r&b", "rnb", "country", "latin",
            "indie", "jazz", "metal", "punk", "soul", "blues", "classical",
            "electronic", "dance", "reggae", "folk", "k-pop", "kpop", "afro",
            "gospel", "funk", "disco", "techno", "house", "ambient", "alternative",
        )
        val genres = items.filter { item ->
            genreKeywords.any { key -> item.title.contains(key, ignoreCase = true) }
        }
        val moods = items.filterNot { it in genres }
        _moodItems.value = moods.ifEmpty { items }
        _genreItems.value = genres.ifEmpty { items }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            explorePage.value = null
            _moodItems.value = emptyList()
            _genreItems.value = emptyList()
            load()
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            load()
        }
    }
}
