package com.mamatiquest.app.models

import androidx.compose.ui.graphics.Color
import java.util.UUID

/**
 * First-party "suggested for you" content shown on Home — the app promoting
 * its own unexplored themes or local multiplayer, chosen from what a player
 * has (and hasn't) already played, entirely on-device. No third-party ad
 * network is involved.
 */
sealed class HouseAdAction {
    data object OpenMultiplayer : HouseAdAction()
    data class OpenTheme(val theme: GameTheme) : HouseAdAction()
}

data class HouseAd(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subtitle: String,
    val icon: String,
    val gradientColors: List<Color>,
    val tintColor: Color,
    val action: HouseAdAction,
)
