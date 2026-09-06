package app.kelkoo.music.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.music.innertube.YouTube
import com.music.innertube.models.YouTubeLocale
import app.kelkoo.music.constants.ContentCountryKey
import app.kelkoo.music.constants.ContentLanguageKey
import app.kelkoo.music.constants.ContentLanguagesKey
import app.kelkoo.music.constants.CountryCodeToName
import app.kelkoo.music.constants.LanguageCodeToName
import app.kelkoo.music.constants.SYSTEM_DEFAULT
import kotlinx.coroutines.flow.first

/**
 * Country + music-language prefs for onboarding and Discover.
 * India is the default region, not a hard lock — [gl] follows the user's country.
 */
object ContentLanguageSupport {
    const val DEFAULT_COUNTRY = "IN"

    /** Popular countries shown first in onboarding (India primary). */
    val popularCountries: List<String> = listOf(
        "IN", "US", "GB", "AE", "SA", "PK", "BD", "LK", "NP",
        "MY", "SG", "ID", "PH", "AU", "CA", "JP", "KR",
        "BR", "MX", "DE", "FR", "NG", "ZA",
    ).filter { it in CountryCodeToName }

    /** Suggested music languages per content country. */
    private val countryLanguageSuggestions: Map<String, List<String>> = mapOf(
        "IN" to listOf("en", "hi", "ml", "ta", "te", "kn", "bn", "mr", "gu", "pa", "ur"),
        "PK" to listOf("ur", "en", "hi", "pa"),
        "BD" to listOf("bn", "en"),
        "LK" to listOf("si", "ta", "en"),
        "NP" to listOf("ne", "en", "hi"),
        "AE" to listOf("ar", "en", "hi", "ml", "ta", "ur"),
        "SA" to listOf("ar", "en", "hi", "ur"),
        "MY" to listOf("ms", "en", "zh-CN", "ta", "hi"),
        "SG" to listOf("en", "zh-CN", "ms", "ta"),
        "ID" to listOf("id", "en"),
        "PH" to listOf("en", "fil"),
        "US" to listOf("en", "es-419", "es"),
        "CA" to listOf("en", "fr-CA", "fr"),
        "GB" to listOf("en-GB", "en"),
        "AU" to listOf("en"),
        "NZ" to listOf("en"),
        "IE" to listOf("en"),
        "JP" to listOf("ja", "en"),
        "KR" to listOf("ko", "en"),
        "BR" to listOf("pt", "en"),
        "PT" to listOf("pt-PT", "en"),
        "MX" to listOf("es-419", "es", "en"),
        "ES" to listOf("es", "en"),
        "AR" to listOf("es-419", "es", "en"),
        "CO" to listOf("es-419", "es", "en"),
        "CL" to listOf("es-419", "es", "en"),
        "DE" to listOf("de", "en"),
        "AT" to listOf("de", "en"),
        "CH" to listOf("de", "fr", "it", "en"),
        "FR" to listOf("fr", "en"),
        "BE" to listOf("fr", "nl", "de", "en"),
        "NL" to listOf("nl", "en"),
        "IT" to listOf("it", "en"),
        "NG" to listOf("en"),
        "ZA" to listOf("en", "zu", "af"),
        "EG" to listOf("ar", "en"),
        "TR" to listOf("tr", "en"),
        "RU" to listOf("ru", "en"),
        "UA" to listOf("uk", "ru", "en"),
        "CN" to listOf("zh-CN", "en"),
        "TW" to listOf("zh-TW", "en"),
        "HK" to listOf("zh-HK", "zh-TW", "en"),
        "TH" to listOf("th", "en"),
        "VN" to listOf("vi", "en"),
    )

    fun countryName(code: String): String =
        CountryCodeToName[code] ?: code

    fun suggestedLanguages(country: String): List<String> {
        val suggested = countryLanguageSuggestions[country]
            ?: listOf("en")
        return suggested.filter { it in LanguageCodeToName || it in friendlyLabels }
    }

    fun defaultLanguagesForCountry(country: String): Set<String> {
        val suggested = suggestedLanguages(country)
        return when {
            suggested.isEmpty() -> setOf("en")
            country == "IN" -> setOf("en") // start light; user adds regional
            else -> setOf(suggested.first())
        }
    }

    fun languageLabel(code: String): String =
        friendlyLabels[code] ?: LanguageCodeToName[code] ?: code

    /** Short English labels for onboarding chips. */
    private val friendlyLabels = mapOf(
        "en" to "English",
        "en-GB" to "English (UK)",
        "hi" to "Hindi",
        "ml" to "Malayalam",
        "ta" to "Tamil",
        "te" to "Telugu",
        "kn" to "Kannada",
        "bn" to "Bengali",
        "mr" to "Marathi",
        "gu" to "Gujarati",
        "pa" to "Punjabi",
        "ur" to "Urdu",
        "si" to "Sinhala",
        "ne" to "Nepali",
        "ar" to "Arabic",
        "ms" to "Malay",
        "id" to "Indonesian",
        "fil" to "Filipino",
        "zh-CN" to "Chinese (Simplified)",
        "zh-TW" to "Chinese (Traditional)",
        "zh-HK" to "Chinese (Hong Kong)",
        "ja" to "Japanese",
        "ko" to "Korean",
        "pt" to "Portuguese (Brazil)",
        "pt-PT" to "Portuguese",
        "es" to "Spanish",
        "es-419" to "Spanish (Latin America)",
        "fr" to "French",
        "fr-CA" to "French (Canada)",
        "de" to "German",
        "it" to "Italian",
        "nl" to "Dutch",
        "tr" to "Turkish",
        "ru" to "Russian",
        "uk" to "Ukrainian",
        "th" to "Thai",
        "vi" to "Vietnamese",
        "af" to "Afrikaans",
        "zu" to "Zulu",
        "sw" to "Swahili",
    )

    fun allLanguageCodes(): List<String> =
        (friendlyLabels.keys + LanguageCodeToName.keys).distinct().sortedBy { languageLabel(it) }

    fun orderedLanguages(selected: Set<String>, country: String): List<String> {
        val suggested = suggestedLanguages(country)
        val rest = selected.filter { it !in suggested }
        return (suggested.filter { it in selected } + rest).ifEmpty {
            selected.toList().ifEmpty { listOf("en") }
        }
    }

    fun primaryLanguage(selected: Set<String>, country: String): String {
        val ordered = orderedLanguages(selected, country)
        // Prefer a non-English pick when the user explicitly chose regional languages.
        val nonEnglish = ordered.firstOrNull { !it.startsWith("en") }
        return nonEnglish ?: ordered.first()
    }

    fun resolveCountry(stored: String?): String {
        val code = stored?.takeIf { it.isNotBlank() && it != SYSTEM_DEFAULT && it != "system" }
        return code?.takeIf { it in CountryCodeToName } ?: DEFAULT_COUNTRY
    }

    fun searchQuery(code: String): String {
        val base = when (code) {
            "en", "en-GB" -> "english songs"
            "hi" -> "hindi songs"
            "ml" -> "malayalam songs"
            "ta" -> "tamil songs"
            "te" -> "telugu songs"
            "kn" -> "kannada songs"
            "bn" -> "bengali songs"
            "mr" -> "marathi songs"
            "gu" -> "gujarati songs"
            "pa" -> "punjabi songs"
            "ur" -> "urdu songs"
            "si" -> "sinhala songs"
            "ne" -> "nepali songs"
            "ar" -> "arabic songs"
            "ms" -> "malay songs"
            "id" -> "indonesian songs"
            "fil" -> "filipino songs"
            "ja" -> "japanese songs"
            "ko" -> "korean songs"
            "zh-CN", "zh-TW", "zh-HK" -> "chinese songs"
            "pt", "pt-PT" -> "portuguese songs"
            "es", "es-419" -> "spanish songs"
            "fr", "fr-CA" -> "french songs"
            "de" -> "german songs"
            "it" -> "italian songs"
            "nl" -> "dutch songs"
            "tr" -> "turkish songs"
            "ru" -> "russian songs"
            "th" -> "thai songs"
            "vi" -> "vietnamese songs"
            else -> "${languageLabel(code)} songs"
        }
        return base
    }

    fun shelfTitle(code: String): String = "${languageLabel(code)} for you"

    suspend fun applyToYouTube(dataStore: DataStore<Preferences>) {
        val prefs = dataStore.data.first()
        val country = resolveCountry(prefs[ContentCountryKey])
        val selected = prefs[ContentLanguagesKey].orEmpty()
            .ifEmpty {
                prefs[ContentLanguageKey]
                    ?.takeIf { it != SYSTEM_DEFAULT && it != "system" }
                    ?.let { setOf(it) }
                    .orEmpty()
            }
            .ifEmpty { defaultLanguagesForCountry(country) }
        val hl = prefs[ContentLanguageKey]
            ?.takeIf { it != SYSTEM_DEFAULT && it != "system" }
            ?: primaryLanguage(selected, country)
        YouTube.locale = YouTubeLocale(gl = country, hl = hl)
    }
}
