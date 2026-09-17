package com.wordquest.app.models

import kotlinx.serialization.Serializable

@Serializable
enum class GameMode {
    WORD_SEARCH, TRIVIA;

    val displayName: String
        get() = when (this) {
            WORD_SEARCH -> "Word Search"
            TRIVIA -> "Trivia"
        }

    /** SF Symbol name, kept as-is from iOS content; mapped to a Material icon in the UI layer. */
    val icon: String
        get() = when (this) {
            WORD_SEARCH -> "square.grid.3x3.fill"
            TRIVIA -> "questionmark.circle.fill"
        }

    companion object {
        const val LEVEL_COUNT = 1000
        const val TIER_SIZE = 100
        const val TIER_COUNT = LEVEL_COUNT / TIER_SIZE
    }
}

@Serializable
data class LevelResult(
    val stars: Int,
    val bestScore: Int,
    val bestTimeSeconds: Int,
)
