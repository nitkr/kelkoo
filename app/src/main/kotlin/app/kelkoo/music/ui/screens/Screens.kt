

package app.kelkoo.music.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import app.kelkoo.music.R

@Immutable
sealed class Screens(
    @StringRes val titleId: Int,
    @DrawableRes val iconIdInactive: Int,
    @DrawableRes val iconIdActive: Int,
    val route: String,
) {
    object Home : Screens(
        titleId = R.string.home,
        iconIdInactive = R.drawable.home_outlined,
        iconIdActive = R.drawable.home_filled,
        route = "home"
    )

    object Explore : Screens(
        titleId = R.string.explore,
        iconIdInactive = R.drawable.explore_outlined,
        iconIdActive = R.drawable.explore_filled,
        route = "explore"
    )

    object Search : Screens(
        titleId = R.string.search,
        iconIdInactive = R.drawable.search,
        iconIdActive = R.drawable.search,
        route = "search_input"
    )

    object ListenTogether : Screens(
        titleId = R.string.together,
        iconIdInactive = R.drawable.group_outlined,
        iconIdActive = R.drawable.group_filled,
        route = "listen_together"
    )

    object Library : Screens(
        titleId = R.string.filter_library,
        iconIdInactive = R.drawable.library_music_outlined,
        iconIdActive = R.drawable.library_music_filled,
        route = "library"
    )

    /** Aura Phase 1 Profile tab — Account screen; Dial is NOT a tab. */
    object Profile : Screens(
        titleId = R.string.account,
        iconIdInactive = R.drawable.person,
        iconIdActive = R.drawable.person,
        route = "account"
    )

    companion object {
        // Aura Phase 1 glass nav: Home | Explore | Library | Profile
        // Search remains reachable from Explore / top bar (Screens.Search).
        val MainScreens = listOf(Home, Explore, Library, Profile)
    }
}
