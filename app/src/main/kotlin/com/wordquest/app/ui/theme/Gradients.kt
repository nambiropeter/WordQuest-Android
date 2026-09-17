package com.wordquest.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.wordquest.app.models.GameTheme

/** Per-theme diagonal gradient, matching iOS's `GameTheme.gradient`. */
fun GameTheme.brush(): Brush = Brush.linearGradient(listOf(primaryColor, secondaryColor))

/** Home screen's Word Search mode card gradient (`#2A9D8F` → `#264653` in iOS's HomeView). */
val WordSearchModeGradient = Brush.linearGradient(listOf(Color(0xFF2A9D8F), Color(0xFF264653)))

/** Home screen's Trivia mode card gradient (`#EF476F` → `#7209B7` in iOS's HomeView). */
val TriviaModeGradient = Brush.linearGradient(listOf(Color(0xFFEF476F), Color(0xFF7209B7)))

/** Local multiplayer card gradient (`#118AB2` → `#073B4C`, reused from `HouseAdProvider.multiplayerAd()`). */
val MultiplayerGradient = Brush.linearGradient(listOf(Color(0xFF118AB2), Color(0xFF073B4C)))
