package app.kelkoo.music.playback

import app.kelkoo.music.models.MediaMetadata

data class LyricsWithProvider(
    val lyrics: String?,
    val providerName: String
)

interface ILyricsHelper {
    suspend fun getLyrics(mediaMetadata: MediaMetadata): LyricsWithProvider
}
