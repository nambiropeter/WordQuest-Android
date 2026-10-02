package com.mamatiquest.app.ui.navigation

import android.net.Uri
import android.os.Bundle
import androidx.navigation.NavType
import com.mamatiquest.app.models.GameMode
import com.mamatiquest.app.models.GameTheme
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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

/**
 * Navigation-Compose's type-safe routes only auto-derive a [NavType] for
 * primitives and [Serializable] enums; a custom data class like [GameTheme]
 * needs one supplied explicitly via `typeMap`, or route registration crashes
 * at startup with "could not find any NavType for argument".
 */
val GameThemeNavType = object : NavType<GameTheme>(isNullableAllowed = false) {
    override fun get(bundle: Bundle, key: String): GameTheme? =
        bundle.getString(key)?.let { Json.decodeFromString(GameTheme.serializer(), it) }

    override fun parseValue(value: String): GameTheme =
        Json.decodeFromString(GameTheme.serializer(), Uri.decode(value))

    override fun serializeAsValue(value: GameTheme): String =
        Uri.encode(Json.encodeToString(GameTheme.serializer(), value))

    override fun put(bundle: Bundle, key: String, value: GameTheme) {
        bundle.putString(key, Json.encodeToString(GameTheme.serializer(), value))
    }
}

val NullableGameThemeNavType = object : NavType<GameTheme?>(isNullableAllowed = true) {
    override fun get(bundle: Bundle, key: String): GameTheme? =
        bundle.getString(key)?.let { Json.decodeFromString(GameTheme.serializer(), it) }

    override fun parseValue(value: String): GameTheme? =
        if (value == "null") null else Json.decodeFromString(GameTheme.serializer(), Uri.decode(value))

    override fun serializeAsValue(value: GameTheme?): String =
        value?.let { Uri.encode(Json.encodeToString(GameTheme.serializer(), it)) } ?: "null"

    override fun put(bundle: Bundle, key: String, value: GameTheme?) {
        bundle.putString(key, value?.let { Json.encodeToString(GameTheme.serializer(), it) })
    }
}
