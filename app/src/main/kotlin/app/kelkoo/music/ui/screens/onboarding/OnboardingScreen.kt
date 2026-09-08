package app.kelkoo.music.ui.screens.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
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
import app.kelkoo.music.constants.LocationFocusKey
import app.kelkoo.music.constants.LocationPersonalizationKey
import app.kelkoo.music.constants.OnboardingCompleteKey
import app.kelkoo.music.constants.SonicProfileMoodsKey
import app.kelkoo.music.ui.aura.AuraBottomAnchoredHero
import app.kelkoo.music.ui.aura.AuraGlassCard
import app.kelkoo.music.ui.aura.AuraGlassPillButton
import app.kelkoo.music.ui.aura.AuraGold
import app.kelkoo.music.ui.aura.AuraPageDots
import app.kelkoo.music.ui.aura.AuraReadableTitle
import app.kelkoo.music.ui.aura.AuraSonicIllustration
import app.kelkoo.music.ui.aura.AuraSyncIllustration
import app.kelkoo.music.ui.aura.AuraTextLink
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
    // Off until Grant Access; focus chips deferred to later prefs (not toggled on this page).
    var locationAllowed by remember { mutableStateOf(false) }
    var sonicMoods by remember { mutableStateOf(setOf<String>()) }
    var locationFocus by remember { mutableStateOf("metro") } // metro | city | travel

    // Sequential grant chain: Location (soft pref) → Notifications → Music/audio.
    var pendingAdvanceAfterGrant by remember { mutableStateOf(false) }

    val mediaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        mediaAllowed = granted
        if (pendingAdvanceAfterGrant) {
            pendingAdvanceAfterGrant = false
            page = 4
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsAllowed = granted
        if (pendingAdvanceAfterGrant) {
            if (!mediaAllowed) {
                mediaLauncher.launch(mediaPermission)
            } else {
                pendingAdvanceAfterGrant = false
                page = 4
            }
        }
    }


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
                prefs[LocationPersonalizationKey] = locationAllowed
                prefs[LocationFocusKey] = locationFocus
                if (sonicMoods.isNotEmpty()) prefs[SonicProfileMoodsKey] = sonicMoods
            }
            YouTube.locale = YouTubeLocale(gl = selectedCountry, hl = primary)
            setAppLocale(context, Locale.forLanguageTag("en"))
            onFinished()
        }
    }

    fun grantAccessAndAdvance() {
        // Location is a soft region-personalization pref (no ACCESS_* declared) — on until Grant.
        locationAllowed = true
        pendingAdvanceAfterGrant = true
        val needNotifications =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsAllowed
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            notificationsAllowed = true
        }
        when {
            needNotifications ->
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            !mediaAllowed ->
                mediaLauncher.launch(mediaPermission)
            else -> {
                pendingAdvanceAfterGrant = false
                page = 4
            }
        }
    }

    // Soft strand feel on locked ribbon plate: slow horizontal phase drift + alpha pulse.
    val ribbonMotion = rememberInfiniteTransition(label = "welcomeRibbon")
    val ribbonDriftDp by ribbonMotion.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ribbonDriftX",
    )
    val ribbonAlpha by ribbonMotion.animateFloat(
        initialValue = 0.88f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ribbonAlpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Image(
            painter = painterResource(R.drawable.bg_gold_waves),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Slight overscale so ±12dp drift never reveals edges under Crop.
                    scaleX = 1.08f
                    scaleY = 1.08f
                    translationX = ribbonDriftDp.dp.toPx()
                    alpha = ribbonAlpha
                },
            contentScale = ContentScale.Crop,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 10.dp)
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
                        selectedMoods = sonicMoods,
                        onToggleMood = { mood ->
                            sonicMoods = sonicMoods.toMutableSet().also { set ->
                                if (!set.add(mood)) set.remove(mood)
                            }
                        },
                        onGetStarted = { page = 3 },
                        onSkip = { page = 3 },
                    )
                    3 -> PermissionsPage(
                        onGrant = { grantAccessAndAdvance() },
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

            // 24.dp between CTA cluster and pager; safe-area already via navBarsPadding.
            AuraPageDots(
                pageCount = PAGE_COUNT,
                current = page,
                modifier = Modifier.padding(top = 24.dp, bottom = 10.dp),
            )
        }
    }
}

@Composable
private fun WelcomePage(onGetStarted: () -> Unit) {
    val gold = Color(0xFFD4AF37)
    val ctaGold = Color(0xFFE4B53D)
    val bodyGray = Color(0xFFB0B0B0)
    val ctaShape = RoundedCornerShape(50)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ~top 40% empty (weight 0.8 vs 1.0 below content)
        Spacer(Modifier.weight(0.8f))

        Text(
            text = "KELKOO",
            color = gold,
            fontWeight = FontWeight.Bold,
            fontSize = 42.sp,
            fontFamily = FontFamily.Default,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.55f),
                    offset = Offset(0f, 4f),
                    blurRadius = 16f,
                ),
            ),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Every Mile. Every Rhythm.\nCurated for the Journey.",
            color = gold,
            fontWeight = FontWeight.Medium,
            fontSize = 22.sp,
            fontFamily = FontFamily.Default,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Unlock curated soundtracks designed\nto perfectly score your path.",
            color = bodyGray,
            fontWeight = FontWeight.Light,
            fontSize = 14.sp,
            fontFamily = FontFamily.Default,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        Spacer(Modifier.weight(1f))

        // Pill CTA: full width within 24.dp horizontal safe pad (parent inset).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(ctaShape)
                .background(ctaGold)
                .clickable(onClick = onGetStarted),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Get Started >",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                fontFamily = FontFamily.Default,
            )
        }
    }
}


@Composable
private fun SyncPage(onNext: () -> Unit) {
    AuraBottomAnchoredHero(
        modifier = Modifier.padding(horizontal = 8.dp),
        cta = {
            AuraGlassPillButton(
                label = stringResource(R.string.onboarding_next),
                onClick = onNext,
                filled = true,
                trailingIcon = R.drawable.navigate_next,
                compact = true,
                modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth(),
            )
        },
    ) {
        AuraReadableTitle(
            text = "Sync Between Phone and Car",
            color = Color.White,
            fontSize = 24,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Spacer(Modifier.height(24.dp))
        AuraSyncIllustration()
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_sync_subtitle),
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun SonicProfilePage(
    selectedMoods: Set<String>,
    onToggleMood: (String) -> Unit,
    onGetStarted: () -> Unit,
    onSkip: () -> Unit,
) {
    val config = LocalConfiguration.current
    val compact = config.screenHeightDp < 720 || config.fontScale > 1.1f
    val moods = listOf(
        "Chill", "Energize", "Focus", "Late Night",
        "Indie", "R&B", "Workout", "Acoustic",
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Hero + chips scroll in the space above fixed bottom CTAs.
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
                AuraReadableTitle(
                    text = "Discover your\nsonic profile",
                    color = Color.White,
                    fontSize = if (compact) 24 else 28,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
                AuraSonicIllustration(height = if (compact) 120.dp else 160.dp)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.onboarding_sonic_subtitle),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Pick moods that feel like you",
                    color = AuraGold.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    moods.forEach { mood ->
                        PreferenceChip(
                            label = mood,
                            selected = mood in selectedMoods,
                            onClick = { onToggleMood(mood) },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
        AuraGlassPillButton(
            label = stringResource(R.string.continue_listening),
            onClick = onGetStarted,
            filled = true,
            trailingIcon = R.drawable.navigate_next,
            compact = true,
            modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth(),
        )
        Spacer(Modifier.height(2.dp))
        AuraTextLink(
            label = stringResource(R.string.onboarding_sonic_skip),
            onClick = onSkip,
        )
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun PermissionsPage(
    onGrant: () -> Unit,
    onMaybeLater: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(8.dp))
                AuraReadableTitle(
                    text = "Enable your journey",
                    color = Color.White,
                    fontSize = 26,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.onboarding_permissions_subtitle),
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Spacer(Modifier.height(20.dp))
                PermissionInfoCard(
                    icon = R.drawable.location_on,
                    title = stringResource(R.string.permission_location_title),
                    description = stringResource(R.string.permission_location_desc),
                )
                Spacer(Modifier.height(12.dp))
                PermissionInfoCard(
                    icon = R.drawable.notification,
                    title = stringResource(R.string.permission_notifications_title),
                    description = stringResource(R.string.permission_notifications_desc),
                )
                Spacer(Modifier.height(12.dp))
                PermissionInfoCard(
                    icon = R.drawable.library_music,
                    title = stringResource(R.string.permission_music_audio_title),
                    description = stringResource(R.string.permission_music_audio_desc),
                )
                Spacer(Modifier.height(12.dp))
            }
        }
        AuraGlassPillButton(
            label = stringResource(R.string.onboarding_grant_access),
            onClick = onGrant,
            filled = true,
            compact = true,
            modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth(),
        )
        Spacer(Modifier.height(2.dp))
        AuraTextLink(
            label = stringResource(R.string.onboarding_maybe_later),
            onClick = onMaybeLater,
        )
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun PermissionInfoCard(
    icon: Int,
    title: String,
    description: String,
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
            ) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    description,
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
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
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start,
        ) {
            Spacer(Modifier.height(12.dp))
            // Shared leading edge: globe left = title = Region "R" = dropdown border.
            Icon(
                painter = painterResource(R.drawable.globe),
                contentDescription = null,
                tint = AuraGold,
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.Start),
            )
            Spacer(Modifier.height(10.dp))
            AuraReadableTitle(
                text = "Choose your\npreferences",
                color = Color.White,
                fontSize = 26,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Start),
            )
            Spacer(Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.onboarding_preferences_region),
                color = AuraGold.copy(alpha = 0.85f),
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Start)
                    .padding(bottom = 6.dp),
            )
            val regionShape = RoundedCornerShape(28.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(regionShape)
                    .background(Color(0xFF1A1A1A).copy(alpha = 0.55f))
                    .border(1.dp, AuraGold.copy(alpha = 0.5f), regionShape)
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
                PreferenceChipGrid(
                    labels = countries.map { ContentLanguageSupport.countryName(it) },
                    selected = { idx -> countries[idx] == selectedCountry },
                    onClick = { idx -> onSelectCountry(countries[idx]) },
                    columns = 2,
                )
            }

            Spacer(Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.onboarding_preferences_language),
                color = AuraGold.copy(alpha = 0.85f),
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Start)
                    .padding(bottom = 6.dp),
            )
            PreferenceChipGrid(
                labels = languageCodes.map { ContentLanguageSupport.languageLabel(it) },
                selected = { idx -> languageCodes[idx] in selectedLanguages },
                onClick = { idx -> onToggleLanguage(languageCodes[idx]) },
                columns = 3,
            )
            // ~20dp under language chip grid so "Show all languages" is not cramped.
            Spacer(Modifier.height(20.dp))
            AuraTextLink(
                label = stringResource(
                    if (showAllLanguages) R.string.onboarding_show_suggested_languages
                    else R.string.onboarding_show_all_languages
                ),
                onClick = { onShowAllLanguages(!showAllLanguages) },
                flushStart = true,
                modifier = Modifier.align(Alignment.Start),
            )
            Spacer(Modifier.height(8.dp))
        }

        // Fixed bottom CTA slot — same rhythm as Welcome / Sync / Sonic / Permissions.
        AuraGlassPillButton(
            label = stringResource(R.string.onboarding_save_preferences),
            onClick = onSave,
            filled = true,
            compact = true,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .widthIn(max = 300.dp)
                .fillMaxWidth(),
        )
        Spacer(Modifier.height(2.dp))
        AuraTextLink(
            label = stringResource(R.string.onboarding_change_later),
            onClick = onChangeLater,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(4.dp))
    }
}

/** Strict equal-width chip grid — no jagged last-row void. */
@Composable
private fun PreferenceChipGrid(
    labels: List<String>,
    selected: (Int) -> Boolean,
    onClick: (Int) -> Unit,
    columns: Int,
) {
    val rows = labels.indices.chunked(columns)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        rows.forEach { indices ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                indices.forEach { idx ->
                    PreferenceChip(
                        label = labels[idx],
                        selected = selected(idx),
                        onClick = { onClick(idx) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(columns - indices.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PreferenceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) AuraGold else Color.Transparent)
            .border(1.dp, AuraGold.copy(alpha = 0.7f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) Color.Black else AuraGold,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
