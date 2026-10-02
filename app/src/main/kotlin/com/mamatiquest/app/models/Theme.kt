package com.mamatiquest.app.models

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

@Serializable
data class GameTheme(
    val id: String,
    val name: String,
    val icon: String,
    val colorPrimary: String,
    val colorSecondary: String,
) {
    val primaryColor: Color get() = Color(colorPrimary.toColorInt())
    val secondaryColor: Color get() = Color(colorSecondary.toColorInt())
}

/** Parses a "#RRGGBB" hex string the same way iOS's `Color(hex:)` extension does. */
fun String.toColorInt(): Long {
    val cleaned = trim('#')
    val value = cleaned.toLong(16)
    return 0xFF000000L or value
}
