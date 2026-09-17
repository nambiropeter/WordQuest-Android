package com.wordquest.app.data

import androidx.compose.ui.graphics.Color
import com.wordquest.app.models.GameMode
import com.wordquest.app.models.GameTheme
import com.wordquest.app.models.HouseAd
import com.wordquest.app.models.HouseAdAction

object HouseAdProvider {
    fun recommendation(contentStore: ContentStore, progressStore: ProgressStore): HouseAd {
        if (progressStore.levelsCompleted <= 0) return multiplayerAd()

        val themes = contentStore.themes
        val leastPlayed = themes.minByOrNull { engagement(progressStore, it) } ?: themes[0]

        return HouseAd(
            title = "Try ${leastPlayed.name}",
            subtitle = "You haven't explored this one yet — a fresh set of levels is waiting.",
            icon = leastPlayed.icon,
            gradientColors = listOf(leastPlayed.primaryColor, leastPlayed.secondaryColor),
            tintColor = leastPlayed.primaryColor,
            action = HouseAdAction.OpenTheme(leastPlayed),
        )
    }

    private fun engagement(progressStore: ProgressStore, theme: GameTheme): Int =
        progressStore.themeLevelsCompleted(GameMode.WORD_SEARCH, theme) +
            progressStore.themeLevelsCompleted(GameMode.TRIVIA, theme)

    fun multiplayerAd(): HouseAd = HouseAd(
        title = "Play together tonight",
        subtitle = "Invite up to 7 friends nearby for a live trivia round — no internet needed.",
        icon = "antenna.radiowaves.left.and.right",
        gradientColors = listOf(Color(0xFF118AB2), Color(0xFF073B4C)),
        tintColor = Color(0xFF118AB2),
        action = HouseAdAction.OpenMultiplayer,
    )
}
