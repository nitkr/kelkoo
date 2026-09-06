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
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import app.kelkoo.music.R
import app.kelkoo.music.constants.ContentCountryKey
import app.kelkoo.music.constants.ContentLanguageKey
import app.kelkoo.music.constants.ContentLanguagesKey
import app.kelkoo.music.constants.OnboardingCompleteKey
import app.kelkoo.music.constants.SYSTEM_DEFAULT
import app.kelkoo.music.ui.theme.DefaultThemeColor
import app.kelkoo.music.utils.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private data class MusicLanguage(val code: String, val label: String)

private val OnboardingLanguages = listOf(
    MusicLanguage("en", "English"),
    MusicLanguage("hi", "Hindi"),
    MusicLanguage("ml", "Malayalam"),
    MusicLanguage("ta", "Tamil"),
    MusicLanguage("te", "Telugu"),
    MusicLanguage("kn", "Kannada"),
    MusicLanguage("bn", "Bengali"),
    MusicLanguage("mr", "Marathi"),
    MusicLanguage("gu", "Gujarati"),
    MusicLanguage("pa", "Punjabi"),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var page by remember { mutableIntStateOf(0) }
    var selectedLanguages by remember { mutableStateOf(setOf("en")) }

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
            val langs = selectedLanguages.ifEmpty { setOf("en") }
            val primary = app.kelkoo.music.utils.ContentLanguageSupport.primaryLanguage(langs)
            val country = app.kelkoo.music.utils.ContentLanguageSupport.contentCountry(
                langs,
                context.dataStore.data.first()[ContentCountryKey]
            )
            context.dataStore.edit { prefs ->
                prefs[ContentLanguagesKey] = langs
                prefs[ContentLanguageKey] = primary
                prefs[ContentCountryKey] = country
                prefs[OnboardingCompleteKey] = true
            }
            // Apply before Home loads so Discover uses the chosen languages.
            com.music.innertube.YouTube.locale = com.music.innertube.models.YouTubeLocale(
                gl = country,
                hl = primary,
            )
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
                    else -> LanguagesPage(
                        selected = selectedLanguages,
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
                repeat(3) { index ->
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
private fun LanguagesPage(
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onFinish: () -> Unit,
) {
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
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OnboardingLanguages.forEach { lang ->
                val isSelected = lang.code in selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggle(lang.code) },
                    label = { Text(lang.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DefaultThemeColor,
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF242424),
                        labelColor = Color.White,
                    ),
                )
            }
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
