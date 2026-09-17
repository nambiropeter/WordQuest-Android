package com.wordquest.app.models

import androidx.compose.ui.graphics.Color
import java.util.UUID

/**
 * First-party promotional content shown in the same slot a real ad fills once
 * one is loaded. Never labeled "Ad" — it's the app promoting itself, so it's
 * tagged "Suggested" instead, and chosen from what a player has (and hasn't)
 * already played, entirely on-device.
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
