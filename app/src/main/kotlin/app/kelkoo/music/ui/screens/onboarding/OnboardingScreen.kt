package app.kelkoo.music.ui.screens.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import app.kelkoo.music.R
import app.kelkoo.music.constants.AppLanguageKey
import app.kelkoo.music.constants.ContentCountryKey
import app.kelkoo.music.constants.ContentLanguageKey
import app.kelkoo.music.constants.ContentLanguagesKey
import app.kelkoo.music.constants.OnboardingCompleteKey
import app.kelkoo.music.ui.aura.AuraGlassCard
import app.kelkoo.music.ui.aura.AuraGlassPillButton
import app.kelkoo.music.ui.aura.AuraGold
import app.kelkoo.music.ui.aura.AuraPageDots
import app.kelkoo.music.ui.aura.AuraSerifTitle
import app.kelkoo.music.ui.aura.AuraSonicIllustration
import app.kelkoo.music.ui.aura.AuraSyncIllustration
import app.kelkoo.music.ui.aura.AuraTextLink
import app.kelkoo.music.ui.aura.AuraVoidBackground
import app.kelkoo.music.ui.aura.AuraWaveform
import app.kelkoo.music.ui.aura.AuraWordmark
import app.kelkoo.music.utils.ContentLanguageSupport
import app.kelkoo.music.utils.dataStore
import app.kelkoo.music.utils.setAppLocale
import com.music.innertube.YouTube
import com.music.innertube.models.YouTubeLocale
import java.util.Locale
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 5

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var page by remember { mutableIntStateOf(0) }
    var selectedCountry by remember { mutableStateOf(ContentLanguageSupport.DEFAULT_COUNTRY) }
    var selectedLanguages by remember {
        mutableStateOf(ContentLanguageSupport.defaultLanguagesForCountry(ContentLanguageSupport.DEFAULT_COUNTRY))
    }
    var showAllCountries by remember { mutableStateOf(false) }
    var showAllLanguages by remember { mutableStateOf(false) }

    val notificationsGranted = remember {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) true
        else ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
    var notificationsAllowed by remember { mutableStateOf(notificationsGranted) }

    val mediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val mediaGranted = remember {
        ContextCompat.checkSelfPermission(context, mediaPermission) == PackageManager.PERMISSION_GRANTED
    }
    var mediaAllowed by remember { mutableStateOf(mediaGranted) }

    // Soft "Location" preference for boards (region personalization) — no undeclared ACCESS_* permission.
    var locationAllowed by remember { mutableStateOf(true) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notificationsAllowed = granted }

    val mediaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> mediaAllowed = granted }

    fun completeOnboarding() {
        scope.launch {
            val langs = selectedLanguages.ifEmpty {
                ContentLanguageSupport.defaultLanguagesForCountry(selectedCountry)
            }
            val primary = ContentLanguageSupport.primaryLanguage(langs, selectedCountry)
            context.dataStore.edit { prefs ->
                prefs[ContentCountryKey] = selectedCountry
                prefs[ContentLanguagesKey] = langs
                prefs[ContentLanguageKey] = primary
                // English-first UI after onboarding
                prefs[AppLanguageKey] = "en"
                prefs[OnboardingCompleteKey] = true
            }
            YouTube.locale = YouTubeLocale(gl = selectedCountry, hl = primary)
            setAppLocale(context, Locale.forLanguageTag("en"))
            onFinished()
        }
    }

    fun requestOutstandingPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsAllowed) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            notificationsAllowed = true
        }
        if (!mediaAllowed) mediaLauncher.launch(mediaPermission)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AuraVoidBackground(Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            AnimatedContent(
                targetState = page,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.weight(1f),
                label = "onboarding_page",
            ) { current ->
                when (current) {
                    0 -> WelcomePage(onGetStarted = { page = 1 })
                    1 -> SyncPage(onNext = { page = 2 })
                    2 -> SonicProfilePage(
                        onGetStarted = { page = 3 },
                        onSkip = { page = 3 },
                    )
                    3 -> PermissionsPage(
                        locationAllowed = locationAllowed,
                        notificationsAllowed = notificationsAllowed,
                        mediaAllowed = mediaAllowed,
                        onToggleLocation = { locationAllowed = !locationAllowed },
                        onToggleNotifications = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (!notificationsAllowed) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                notificationsAllowed = true
                            }
                        },
                        onToggleMedia = {
                            if (!mediaAllowed) mediaLauncher.launch(mediaPermission)
                        },
                        onGrant = {
                            requestOutstandingPermissions()
                            page = 4
                        },
                        onMaybeLater = { page = 4 },
                    )
                    else -> PreferencesPage(
                        selectedCountry = selectedCountry,
                        selectedLanguages = selectedLanguages,
                        showAllCountries = showAllCountries,
                        showAllLanguages = showAllLanguages,
                        onShowAllCountries = { showAllCountries = it },
                        onShowAllLanguages = { showAllLanguages = it },
                        onSelectCountry = { code ->
                            selectedCountry = code
                            selectedLanguages =
                                ContentLanguageSupport.defaultLanguagesForCountry(code)
                            showAllLanguages = false
                        },
                        onToggleLanguage = { code ->
                            selectedLanguages = selectedLanguages.toMutableSet().also { set ->
                                if (!set.add(code)) {
                                    if (set.size > 1) set.remove(code)
                                }
                            }
                        },
                        onSave = { completeOnboarding() },
                        onChangeLater = { completeOnboarding() },
                    )
                }
            }

            if (page <= 1) {
                AuraPageDots(pageCount = PAGE_COUNT, current = page, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            } else if (page == 2) {
                // Sonic has its own CTAs; still show dots for continuity
                AuraPageDots(pageCount = PAGE_COUNT, current = page, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
            } else {
                AuraPageDots(pageCount = PAGE_COUNT, current = page, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            }
        }
    }
}

@Composable
private fun WelcomePage(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        AuraWordmark(showMusicLabel = false, fontSize = 20)
        Spacer(Modifier.height(28.dp))
        Text(
            text = "Welcome to",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        AuraSerifTitle(
            text = "KELKOO",
            color = AuraGold,
            fontSize = 40,
            letterSpacing = 6f,
        )
        Spacer(Modifier.height(36.dp))
        AuraWaveform(height = 88.dp)
        Spacer(Modifier.height(12.dp))
        Icon(
            painter = painterResource(R.drawable.music_note),
            contentDescription = null,
            tint = AuraGold,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.onboarding_welcome_subtitle),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Spacer(Modifier.weight(1f))
        AuraGlassPillButton(
            label = stringResource(R.string.get_started),
            onClick = onGetStarted,
            filled = false,
            leadingIcon = R.drawable.navigate_next,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SyncPage(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        AuraWordmark(showMusicLabel = true, fontSize = 20)
        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(R.string.onboarding_sync_title),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Spacer(Modifier.height(28.dp))
        AuraSyncIllustration()
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.onboarding_sync_subtitle),
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        AuraGlassPillButton(
            label = stringResource(R.string.onboarding_next),
            onClick = onNext,
            filled = true,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SonicProfilePage(
    onGetStarted: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        AuraWordmark(showMusicLabel = true, fontSize = 20)
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Discover Your",
            color = Color.White,
            fontSize = 22.sp,
            textAlign = TextAlign.Center,
        )
        AuraSerifTitle(
            text = "Sonic Profile",
            color = AuraGold,
            fontSize = 32,
        )
        Spacer(Modifier.height(12.dp))
        AuraSonicIllustration()
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.onboarding_sonic_subtitle),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.weight(1f))
        AuraGlassPillButton(
            label = stringResource(R.string.get_started),
            onClick = onGetStarted,
            filled = false,
            leadingIcon = R.drawable.music_note,
        )
        AuraTextLink(
            label = stringResource(R.string.onboarding_sonic_skip) + " ›",
            onClick = onSkip,
        )
    }
}

@Composable
private fun PermissionsPage(
    locationAllowed: Boolean,
    notificationsAllowed: Boolean,
    mediaAllowed: Boolean,
    onToggleLocation: () -> Unit,
    onToggleNotifications: () -> Unit,
    onToggleMedia: () -> Unit,
    onGrant: () -> Unit,
    onMaybeLater: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))
        AuraWordmark(showMusicLabel = true, fontSize = 18)
        Spacer(Modifier.height(20.dp))
        AuraSerifTitle(
            text = stringResource(R.string.onboarding_permissions_title),
            fontSize = 28,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_permissions_subtitle),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        PermissionToggleCard(
            icon = R.drawable.location_on,
            title = stringResource(R.string.permission_location_title),
            description = stringResource(R.string.permission_location_desc),
            checked = locationAllowed,
            onCheckedChange = { onToggleLocation() },
        )
        Spacer(Modifier.height(12.dp))
        PermissionToggleCard(
            icon = R.drawable.notification,
            title = stringResource(R.string.permission_notifications_title),
            description = stringResource(R.string.permission_notifications_desc),
            checked = notificationsAllowed,
            onCheckedChange = { if (it) onToggleNotifications() },
        )
        Spacer(Modifier.height(12.dp))
        PermissionToggleCard(
            icon = R.drawable.library_music,
            title = stringResource(R.string.permission_music_audio_title),
            description = stringResource(R.string.permission_music_audio_desc),
            checked = mediaAllowed,
            onCheckedChange = { if (it) onToggleMedia() },
        )
        Spacer(Modifier.height(28.dp))
        AuraGlassPillButton(
            label = stringResource(R.string.onboarding_grant_access),
            onClick = onGrant,
            filled = true,
        )
        AuraTextLink(
            label = stringResource(R.string.onboarding_maybe_later),
            onClick = onMaybeLater,
        )
    }
}

@Composable
private fun PermissionToggleCard(
    icon: Int,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    AuraGlassCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .border(1.dp, AuraGold.copy(alpha = 0.6f), CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = AuraGold,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(
                    description,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = AuraGold,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.7f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.15f),
                ),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreferencesPage(
    selectedCountry: String,
    selectedLanguages: Set<String>,
    showAllCountries: Boolean,
    showAllLanguages: Boolean,
    onShowAllCountries: (Boolean) -> Unit,
    onShowAllLanguages: (Boolean) -> Unit,
    onSelectCountry: (String) -> Unit,
    onToggleLanguage: (String) -> Unit,
    onSave: () -> Unit,
    onChangeLater: () -> Unit,
) {
    val countries = if (showAllCountries) {
        ContentLanguageSupport.popularCountries +
            app.kelkoo.music.constants.CountryCodeToName.keys
                .filter { it !in ContentLanguageSupport.popularCountries }
                .sortedBy { ContentLanguageSupport.countryName(it) }
    } else {
        ContentLanguageSupport.popularCountries
    }
    val languageCodes = if (showAllLanguages) {
        ContentLanguageSupport.allLanguageCodes()
    } else {
        ContentLanguageSupport.suggestedLanguages(selectedCountry)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))
        AuraWordmark(showMusicLabel = true, fontSize = 18)
        Spacer(Modifier.height(16.dp))
        Icon(
            painter = painterResource(R.drawable.globe),
            contentDescription = null,
            tint = AuraGold,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(16.dp))
        AuraSerifTitle(
            text = stringResource(R.string.onboarding_preferences_title),
            color = AuraGold,
            fontSize = 26,
        )
        Spacer(Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.onboarding_preferences_region),
            color = AuraGold.copy(alpha = 0.85f),
            fontSize = 12.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
        )
        val regionShape = RoundedCornerShape(28.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(regionShape)
                .background(Color(0xFF1A1A1A).copy(alpha = 0.7f))
                .border(1.dp, AuraGold.copy(alpha = 0.45f), regionShape)
                .clickable { onShowAllCountries(!showAllCountries) }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.location_on),
                contentDescription = null,
                tint = AuraGold,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text = ContentLanguageSupport.countryName(selectedCountry),
                color = Color.White,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(R.drawable.arrow_downward),
                contentDescription = null,
                tint = AuraGold,
                modifier = Modifier.size(18.dp),
            )
        }
        if (showAllCountries) {
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                countries.forEach { code ->
                    PreferenceChip(
                        label = ContentLanguageSupport.countryName(code),
                        selected = code == selectedCountry,
                        onClick = { onSelectCountry(code) },
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.onboarding_preferences_language),
            color = AuraGold.copy(alpha = 0.85f),
            fontSize = 12.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            languageCodes.forEach { code ->
                PreferenceChip(
                    label = ContentLanguageSupport.languageLabel(code),
                    selected = code in selectedLanguages,
                    onClick = { onToggleLanguage(code) },
                )
            }
        }
        AuraTextLink(
            label = stringResource(
                if (showAllLanguages) R.string.onboarding_show_suggested_languages
                else R.string.onboarding_show_all_languages
            ),
            onClick = { onShowAllLanguages(!showAllLanguages) },
        )

        Spacer(Modifier.height(20.dp))
        AuraGlassPillButton(
            label = stringResource(R.string.onboarding_save_preferences),
            onClick = onSave,
            filled = false,
            leadingIcon = R.drawable.music_note,
        )
        AuraTextLink(
            label = stringResource(R.string.onboarding_change_later),
            onClick = onChangeLater,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PreferenceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) AuraGold else Color.Transparent)
            .border(1.dp, AuraGold.copy(alpha = 0.7f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            color = if (selected) Color.Black else AuraGold,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
        )
    }
}
