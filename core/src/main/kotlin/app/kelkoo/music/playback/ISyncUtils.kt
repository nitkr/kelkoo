package app.kelkoo.music.playback

import app.kelkoo.music.db.entities.SongEntity

interface ISyncUtils {
    fun likeSong(song: SongEntity)
}
