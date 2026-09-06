package app.kelkoo.music.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.music.innertube.YouTube
import com.music.innertube.models.YouTubeLocale
import app.kelkoo.music.constants.ContentCountryKey
import app.kelkoo.music.constants.ContentLanguageKey
import app.kelkoo.music.constants.ContentLanguagesKey
import app.kelkoo.music.constants.SYSTEM_DEFAULT
import kotlinx.coroutines.flow.first

/**
 * Onboarding music-language helpers. YouTube Music mainly keys off [hl]/[gl];
 * multi-select is honored by loading per-language song shelves on Home/Discover.
 */
object ContentLanguageSupport {
    private val INDIC = setOf("hi", "ml", "ta", "te", "kn", "bn", "mr", "gu", "pa")

    /** Prefer regional languages over English when both were picked. */
    private val PRIMARY_ORDER = listOf(
        "hi", "ml", "ta", "te", "kn", "bn", "mr", "gu", "pa", "en"
    )

    val labels = mapOf(
        "en" to "English",
        "hi" to "Hindi",
        "ml" to "Malayalam",
        "ta" to "Tamil",
        "te" to "Telugu",
        "kn" to "Kannada",
        "bn" to "Bengali",
        "mr" to "Marathi",
        "gu" to "Gujarati",
        "pa" to "Punjabi",
    )

    fun orderedLanguages(selected: Set<String>): List<String> =
        PRIMARY_ORDER.filter { it in selected }.ifEmpty {
            selected.toList().ifEmpty { listOf("en") }
        }

    fun primaryLanguage(selected: Set<String>): String =
        orderedLanguages(selected).first()

    fun contentCountry(selected: Set<String>, current: String?): String {
        if (selected.any { it in INDIC }) return "IN"
        val cur = current?.takeIf { it.isNotBlank() && it != SYSTEM_DEFAULT && it != "system" }
        return cur ?: "US"
    }

    fun searchQuery(code: String): String = when (code) {
        "en" -> "english songs"
        "hi" -> "hindi songs"
        "ml" -> "malayalam songs"
        "ta" -> "tamil songs"
        "te" -> "telugu songs"
        "kn" -> "kannada songs"
        "bn" -> "bengali songs"
        "mr" -> "marathi songs"
        "gu" -> "gujarati songs"
        "pa" -> "punjabi songs"
        else -> "${labels[code] ?: code} songs"
    }

    fun shelfTitle(code: String): String {
        val label = labels[code] ?: code
        return "$label for you"
    }

    suspend fun applyToYouTube(dataStore: DataStore<Preferences>) {
        val prefs = dataStore.data.first()
        val selected = prefs[ContentLanguagesKey].orEmpty()
            .ifEmpty {
                prefs[ContentLanguageKey]
                    ?.takeIf { it != SYSTEM_DEFAULT && it != "system" }
                    ?.let { setOf(it) }
                    .orEmpty()
            }
            .ifEmpty { setOf("en") }
        val hl = prefs[ContentLanguageKey]
            ?.takeIf { it != SYSTEM_DEFAULT && it != "system" }
            ?: primaryLanguage(selected)
        val gl = contentCountry(selected, prefs[ContentCountryKey])
        YouTube.locale = YouTubeLocale(gl = gl, hl = hl)
    }
}
