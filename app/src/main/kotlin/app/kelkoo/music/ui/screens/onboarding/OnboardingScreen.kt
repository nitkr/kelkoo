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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import app.kelkoo.music.R
import app.kelkoo.music.constants.ContentCountryKey
import app.kelkoo.music.constants.ContentLanguageKey
import app.kelkoo.music.constants.ContentLanguagesKey
import app.kelkoo.music.constants.CountryCodeToName
import app.kelkoo.music.constants.OnboardingCompleteKey
import app.kelkoo.music.ui.theme.DefaultThemeColor
import app.kelkoo.music.utils.ContentLanguageSupport
import app.kelkoo.music.utils.dataStore
import com.music.innertube.YouTube
import com.music.innertube.models.YouTubeLocale
import kotlinx.coroutines.launch

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
                prefs[OnboardingCompleteKey] = true
            }
            YouTube.locale = YouTubeLocale(gl = selectedCountry, hl = primary)
            onFinished()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212),
        contentColor = Color.White,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            AnimatedContent(
                targetState = page,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.weight(1f),
                label = "onboarding_page",
            ) { current ->
                when (current) {
                    0 -> WelcomePage(onGetStarted = { page = 1 })
                    1 -> PermissionsPage(
                        notificationsAllowed = notificationsAllowed,
                        mediaAllowed = mediaAllowed,
                        onAllowNotifications = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                notificationsAllowed = true
                            }
                        },
                        onAllowMedia = { mediaLauncher.launch(mediaPermission) },
                        onSkip = { page = 2 },
                        onNext = { page = 2 },
                    )
                    2 -> CountryPage(
                        selected = selectedCountry,
                        showAll = showAllCountries,
                        onShowAllChange = { showAllCountries = it },
                        onSelect = { code ->
                            selectedCountry = code
                            selectedLanguages =
                                ContentLanguageSupport.defaultLanguagesForCountry(code)
                            showAllLanguages = false
                        },
                        onNext = { page = 3 },
                    )
                    else -> LanguagesPage(
                        country = selectedCountry,
                        selected = selectedLanguages,
                        showAll = showAllLanguages,
                        onShowAllChange = { showAllLanguages = it },
                        onToggle = { code ->
                            selectedLanguages = selectedLanguages.toMutableSet().also { set ->
                                if (!set.add(code)) {
                                    if (set.size > 1) set.remove(code)
                                }
                            }
                        },
                        onFinish = { completeOnboarding() },
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (index == page) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == page) DefaultThemeColor
                                else Color.White.copy(alpha = 0.25f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomePage(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_nobg),
            contentDescription = null,
            modifier = Modifier
                .size(112.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF1A1A1A))
                .padding(16.dp),
        )
        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(R.string.welcome_to_kelkoo),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.onboarding_welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onGetStarted,
            colors = ButtonDefaults.buttonColors(
                containerColor = DefaultThemeColor,
                contentColor = Color.Black,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                text = stringResource(R.string.get_started),
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun PermissionsPage(
    notificationsAllowed: Boolean,
    mediaAllowed: Boolean,
    onAllowNotifications: () -> Unit,
    onAllowMedia: () -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.onboarding_permissions_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.onboarding_permissions_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(8.dp))
        PermissionCard(
            title = stringResource(R.string.permission_notifications_title),
            description = stringResource(R.string.permission_notifications_desc),
            granted = notificationsAllowed,
            onAllow = onAllowNotifications,
        )
        PermissionCard(
            title = stringResource(R.string.permission_music_audio_title),
            description = stringResource(R.string.permission_music_audio_desc),
            granted = mediaAllowed,
            onAllow = onAllowMedia,
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(
                containerColor = DefaultThemeColor,
                contentColor = Color.Black,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(stringResource(R.string.onboarding_next), fontWeight = FontWeight.SemiBold)
        }
        TextButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.not_now), color = Color.White.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    granted: Boolean,
    onAllow: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1A1A1A))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Text(description, color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (granted) {
                Text(
                    text = stringResource(R.string.permission_status_allowed),
                    color = DefaultThemeColor,
                    fontWeight = FontWeight.Medium,
                )
            } else {
                Button(
                    onClick = onAllow,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DefaultThemeColor,
                        contentColor = Color.Black,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(stringResource(R.string.allow))
                }
                OutlinedButton(
                    onClick = {},
                    shape = RoundedCornerShape(12.dp),
                    enabled = false,
                ) {
                    Text(stringResource(R.string.not_now), color = Color.White.copy(alpha = 0.5f))
                }
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CountryPage(
    selected: String,
    showAll: Boolean,
    onShowAllChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit,
    onNext: () -> Unit,
) {
    val countries = if (showAll) {
        CountryCodeToName.keys.sortedBy { ContentLanguageSupport.countryName(it) }
    } else {
        ContentLanguageSupport.popularCountries
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.onboarding_country_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.onboarding_country_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            countries.forEach { code ->
                val isSelected = code == selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(code) },
                    label = { Text(ContentLanguageSupport.countryName(code)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DefaultThemeColor,
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF242424),
                        labelColor = Color.White,
                    ),
                )
            }
        }
        TextButton(onClick = { onShowAllChange(!showAll) }) {
            Text(
                text = stringResource(
                    if (showAll) R.string.onboarding_popular_countries
                    else R.string.onboarding_more_countries
                ),
                color = DefaultThemeColor,
            )
        }
        Spacer(modifier.height(16.dp))
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(
                containerColor = DefaultThemeColor,
                contentColor = Color.Black,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(stringResource(R.string.onboarding_next), fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LanguagesPage(
    country: String,
    selected: Set<String>,
    showAll: Boolean,
    onShowAllChange: (Boolean) -> Unit,
    onToggle: (String) -> Unit,
    onFinish: () -> Unit,
) {
    val codes = if (showAll) {
        ContentLanguageSupport.allLanguageCodes()
    } else {
        ContentLanguageSupport.suggestedLanguages(country)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.onboarding_languages_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.onboarding_languages_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
        )
        Text(
            text = ContentLanguageSupport.countryName(country),
            style = MaterialTheme.typography.labelLarge,
            color = DefaultThemeColor,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            codes.forEach { code ->
                val isSelected = code in selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggle(code) },
                    label = { Text(ContentLanguageSupport.languageLabel(code)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DefaultThemeColor,
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF242424),
                        labelColor = Color.White,
                    ),
                )
            }
        }
        TextButton(onClick = { onShowAllChange(!showAll) }) {
            Text(
                text = stringResource(
                    if (showAll) R.string.onboarding_show_suggested_languages
                    else R.string.onboarding_show_all_languages
                ),
                color = DefaultThemeColor,
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onFinish,
            enabled = selected.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(
                containerColor = DefaultThemeColor,
                contentColor = Color.Black,
                disabledContainerColor = Color(0xFF333333),
                disabledContentColor = Color.White.copy(alpha = 0.4f),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(stringResource(R.string.onboarding_finish), fontWeight = FontWeight.SemiBold)
        }
    }
}
