package com.mamatiquest.app.services

import com.mamatiquest.app.models.GameMode

/**
 * Android counterpart to iOS's GameCenterManager (Play Games Services). Data
 * layer code (ProgressStore) reports through this interface so it never
 * depends on the concrete Play Games SDK, which is wired up in a later phase.
 */
interface GameServicesReporter {
    fun submitScore(score: Int, leaderboardId: String)
    fun reportAchievement(achievementId: String, percentComplete: Double = 100.0)

    object LeaderboardId {
        const val WORD_SEARCH_TOTAL_SCORE = "wq_word_search_total_score"
        const val TRIVIA_TOTAL_SCORE = "wq_trivia_total_score"
        const val TOTAL_STARS = "wq_total_stars"
    }

    object AchievementId {
        const val FIRST_WIN = "wq_first_win"
        const val HUNDRED_LEVELS = "wq_hundred_levels"
        const val PERFECT_TRIVIA = "wq_perfect_trivia"
        const val MULTIPLAYER_CHAMPION = "wq_multiplayer_champion"
    }
}

fun GameServicesReporter.leaderboardFor(mode: GameMode): String = when (mode) {
    GameMode.WORD_SEARCH -> GameServicesReporter.LeaderboardId.WORD_SEARCH_TOTAL_SCORE
    GameMode.TRIVIA -> GameServicesReporter.LeaderboardId.TRIVIA_TOTAL_SCORE
}

/** Used until the Play Games Services integration lands (phase 3+). */
object NoOpGameServicesReporter : GameServicesReporter {
    override fun submitScore(score: Int, leaderboardId: String) = Unit
    override fun reportAchievement(achievementId: String, percentComplete: Double) = Unit
}
