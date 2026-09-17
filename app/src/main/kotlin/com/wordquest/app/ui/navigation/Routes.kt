package com.wordquest.app.ui.navigation

import com.wordquest.app.models.GameMode
import com.wordquest.app.models.GameTheme
import kotlinx.serialization.Serializable

/**
 * Type-safe Navigation-Compose routes, mirroring iOS's `AppRoute` enum
 * (`App/AppRoute.swift`) one case at a time.
 */
@Serializable
object HomeRoute

@Serializable
data class ThemeSelectRoute(val mode: GameMode)

@Serializable
data class LevelMapRoute(val mode: GameMode, val theme: GameTheme)

@Serializable
data class WordSearchGameRoute(val theme: GameTheme, val level: Int)

@Serializable
data class TriviaGameRoute(val theme: GameTheme, val level: Int)

@Serializable
object SettingsRoute

@Serializable
data class MultiplayerRoute(val theme: GameTheme? = null)
