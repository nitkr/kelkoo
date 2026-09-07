package app.kelkoo.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.kelkoo.music.constants.HideYoutubeShortsKey
import app.kelkoo.music.ui.utils.resize
import app.kelkoo.music.utils.dataStore
import app.kelkoo.music.utils.get
import app.kelkoo.music.utils.reportException
import com.music.innertube.YouTube
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.filterYoutubeShorts
import com.music.innertube.utils.completed
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

enum class AccountContentType {
    PLAYLISTS, ALBUMS, ARTISTS
}

@HiltViewModel
class AccountViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {
    val playlists = MutableStateFlow<List<PlaylistItem>?>(null)
    val albums = MutableStateFlow<List<AlbumItem>?>(null)
    val artists = MutableStateFlow<List<ArtistItem>?>(null)
    val isLoading = MutableStateFlow(true)
    val selectedContentType = MutableStateFlow(AccountContentType.PLAYLISTS)

    private suspend fun loadPlaylists() {
        val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
        YouTube.library("FEmusic_liked_playlists").completed().onSuccess {
            playlists.value = it.items.filterIsInstance<PlaylistItem>()
                .filterNot { it.id == "SE" }
                .filterYoutubeShorts(hideYoutubeShorts)
        }.onFailure {
            reportException(it)
            // Never leave null forever — Profile tab was blank/stuck on shimmer
            if (playlists.value == null) playlists.value = emptyList()
        }
    }

    init {
        viewModelScope.launch {
            isLoading.value = true
            try {
                loadPlaylists()
                YouTube.library("FEmusic_liked_albums").completed().onSuccess {
                    albums.value = it.items.filterIsInstance<AlbumItem>()
                }.onFailure {
                    reportException(it)
                    if (albums.value == null) albums.value = emptyList()
                }
                YouTube.library("FEmusic_library_corpus_artists").completed().onSuccess {
                    artists.value = it.items.filterIsInstance<ArtistItem>().map { artist ->
                        artist.copy(
                            thumbnail = artist.thumbnail?.resize(1200, 1200)
                        )
                    }
                }.onFailure {
                    reportException(it)
                    if (artists.value == null) artists.value = emptyList()
                }
            } finally {
                // Ensure non-null empties so UI leaves shimmer
                if (playlists.value == null) playlists.value = emptyList()
                if (albums.value == null) albums.value = emptyList()
                if (artists.value == null) artists.value = emptyList()
                isLoading.value = false
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.data
                .map { (try { it[HideYoutubeShortsKey] } catch (_: Exception) { null }) ?: false }
                .distinctUntilChanged()
                .collect {
                    if (playlists.value != null) {
                        loadPlaylists()
                    }
                }
        }
    }

    fun setSelectedContentType(contentType: AccountContentType) {
        selectedContentType.value = contentType
    }
}
