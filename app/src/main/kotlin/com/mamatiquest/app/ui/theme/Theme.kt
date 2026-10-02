package com.mamatiquest.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.mamatiquest.app.services.AppAppearance

// onPrimary/onSecondary must be set alongside primary/secondary. Material's
// defaults are tuned to its own baseline palette — the dark scheme in
// particular assumes a light primary and pairs it with a dark on-color — so
// overriding only primary left buttons rendering dark-on-purple in dark mode
// while light mode used white. WordQuestPurple is dark enough for white
// content in both schemes, which also keeps the two consistent.
private val LightColors = lightColorScheme(
    primary = WordQuestPurple,
    onPrimary = Color.White,
    secondary = WordQuestPurpleDark,
    onSecondary = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = WordQuestPurple,
    onPrimary = Color.White,
    secondary = WordQuestPurpleDark,
    onSecondary = Color.White,
)

/**
 * [appearance] mirrors iOS's `SettingsStore.appearance` /
 * `.preferredColorScheme(...)`: SYSTEM follows the OS setting, LIGHT/DARK force it.
 */
@Composable
fun WordQuestTheme(
    appearance: AppAppearance = AppAppearance.SYSTEM,
    content: @Composable () -> Unit,
) {
    val useDark = when (appearance) {
        AppAppearance.SYSTEM -> isSystemInDarkTheme()
        AppAppearance.LIGHT -> false
        AppAppearance.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (useDark) DarkColors else LightColors,
        content = content,
    )
}
