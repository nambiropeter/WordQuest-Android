package com.wordquest.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.wordquest.app.services.AppAppearance

private val LightColors = lightColorScheme(
    primary = WordQuestPurple,
    secondary = WordQuestPurpleDark,
)

private val DarkColors = darkColorScheme(
    primary = WordQuestPurple,
    secondary = WordQuestPurpleDark,
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
