package com.mamatiquest.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mamatiquest.app.models.GameMode
import com.mamatiquest.app.models.GameTheme
import com.mamatiquest.app.models.LevelResult
import com.mamatiquest.app.services.GameServicesReporter
import com.mamatiquest.app.services.NoOpGameServicesReporter
import com.mamatiquest.app.services.leaderboardFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.progressDataStore: DataStore<Preferences> by preferencesDataStore(name = "wq_progress")

/**
 * Android counterpart to iOS's ProgressStore. Same key scheme, unlock rules,
 * and coin/star accounting as the Swift original. iOS additionally merges
 * against NSUbiquitousKeyValueStore (iCloud) for cross-device sync; there is
 * no Android equivalent wired up yet, so this phase is DataStore-only
 * (single-device local save) — Play Games "Saved Games" cloud sync is a
 * candidate for a later phase, not attempted here.
 */
class ProgressStore private constructor(
    context: Context,
    private val gameServices: GameServicesReporter,
) {
    private val dataStore = context.applicationContext.progressDataStore
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _wordSearchResults = MutableStateFlow<Map<String, LevelResult>>(emptyMap())
    val wordSearchResults: StateFlow<Map<String, LevelResult>> = _wordSearchResults.asStateFlow()

    private val _triviaResults = MutableStateFlow<Map<String, LevelResult>>(emptyMap())
    val triviaResults: StateFlow<Map<String, LevelResult>> = _triviaResults.asStateFlow()

    private val _totalCoins = MutableStateFlow(0)
    val totalCoins: StateFlow<Int> = _totalCoins.asStateFlow()

    init {
        scope.launch { load() }
    }

    private suspend fun load() {
        val prefs = dataStore.data.first()
        _wordSearchResults.value = decode(prefs[WORD_SEARCH_KEY])
        _triviaResults.value = decode(prefs[TRIVIA_KEY])
        _totalCoins.value = prefs[COINS_KEY] ?: 0
    }

    private fun decode(raw: String?): Map<String, LevelResult> {
        if (raw.isNullOrEmpty()) return emptyMap()
        return runCatching { json.decodeFromString<Map<String, LevelResult>>(raw) }.getOrDefault(emptyMap())
    }

    private fun key(theme: GameTheme, level: Int) = "${theme.id}#$level"

    private fun levels(results: Map<String, LevelResult>, theme: GameTheme): List<Int> {
        val prefix = "${theme.id}#"
        return results.keys.filter { it.startsWith(prefix) }.mapNotNull { it.removePrefix(prefix).toIntOrNull() }
    }

    /**
     * Every difficulty tier's first level is always open, so players can jump
     * straight to the difficulty they want instead of grinding earlier tiers.
     * Levels after that within the same tier still unlock sequentially as you
     * complete them, so choosing a tier still feels like progress.
     */
    private fun tierBand(level: Int) = (level - 1) / GameMode.TIER_SIZE

    private fun tierRange(band: Int): IntRange {
        val start = band * GameMode.TIER_SIZE + 1
        val end = minOf(start + GameMode.TIER_SIZE - 1, GameMode.LEVEL_COUNT)
        return start..end
    }

    fun highestUnlockedLevel(mode: GameMode, theme: GameTheme, band: Int): Int {
        val range = tierRange(band)
        val results = resultsFor(mode)
        val completedInTier = levels(results, theme).filter { it in range }
        val maxCompleted = completedInTier.maxOrNull() ?: return range.first
        return minOf(maxCompleted + 1, range.last)
    }

    fun isUnlocked(level: Int, mode: GameMode, theme: GameTheme): Boolean {
        val band = tierBand(level)
        if (level == tierRange(band).first) return true
        return level <= highestUnlockedLevel(mode, theme, band)
    }

    /** The furthest level reached overall, used only to auto-scroll the level map to a "resume here" spot. */
    fun furthestReachedLevel(mode: GameMode, theme: GameTheme): Int {
        val results = resultsFor(mode)
        val maxCompleted = levels(results, theme).maxOrNull() ?: return 1
        return minOf(maxCompleted + 1, GameMode.LEVEL_COUNT)
    }

    fun result(level: Int, mode: GameMode, theme: GameTheme): LevelResult? =
        resultsFor(mode)[key(theme, level)]

    fun themeStars(mode: GameMode, theme: GameTheme): Int {
        val prefix = "${theme.id}#"
        return resultsFor(mode).filterKeys { it.startsWith(prefix) }.values.sumOf { it.stars }
    }

    fun themeLevelsCompleted(mode: GameMode, theme: GameTheme): Int {
        val prefix = "${theme.id}#"
        return resultsFor(mode).keys.count { it.startsWith(prefix) }
    }

    fun recordCompletion(level: Int, mode: GameMode, theme: GameTheme, stars: Int, score: Int, timeSeconds: Int) {
        scope.launch {
            val k = key(theme, level)
            val results = resultsFor(mode)
            val existing = results[k]
            val bestStars = maxOf(existing?.stars ?: 0, stars)
            val bestScore = maxOf(existing?.bestScore ?: 0, score)
            val bestTime = existing?.let { minOf(it.bestTimeSeconds, timeSeconds) } ?: timeSeconds
            val newResult = LevelResult(bestStars, bestScore, bestTime)
            val updated = results + (k to newResult)

            if (mode == GameMode.WORD_SEARCH) _wordSearchResults.value = updated else _triviaResults.value = updated
            persist(mode, updated)

            val earnedCoins = stars * 10 + if (existing == null) 20 else 0
            val newTotal = _totalCoins.value + earnedCoins
            _totalCoins.value = newTotal
            dataStore.edit { it[COINS_KEY] = newTotal }

            reportToGameServices(mode, isFirstCompletion = existing == null)
        }
    }

    private fun reportToGameServices(mode: GameMode, isFirstCompletion: Boolean) {
        val modeTotal = if (mode == GameMode.WORD_SEARCH) wordSearchTotalScore else triviaTotalScore
        gameServices.submitScore(modeTotal, gameServices.leaderboardFor(mode))
        gameServices.submitScore(totalStars, GameServicesReporter.LeaderboardId.TOTAL_STARS)

        if (isFirstCompletion) {
            gameServices.reportAchievement(GameServicesReporter.AchievementId.FIRST_WIN)
        }
        gameServices.reportAchievement(
            GameServicesReporter.AchievementId.HUNDRED_LEVELS,
            percentComplete = minOf(100.0, levelsCompleted.toDouble()),
        )
    }

    private fun resultsFor(mode: GameMode) =
        if (mode == GameMode.WORD_SEARCH) _wordSearchResults.value else _triviaResults.value

    private suspend fun persist(mode: GameMode, results: Map<String, LevelResult>) {
        val key = if (mode == GameMode.WORD_SEARCH) WORD_SEARCH_KEY else TRIVIA_KEY
        dataStore.edit { it[key] = json.encodeToString(results) }
    }

    val wordSearchTotalScore: Int get() = _wordSearchResults.value.values.sumOf { it.bestScore }
    val triviaTotalScore: Int get() = _triviaResults.value.values.sumOf { it.bestScore }
    val totalStars: Int get() = _wordSearchResults.value.values.sumOf { it.stars } + _triviaResults.value.values.sumOf { it.stars }
    val levelsCompleted: Int get() = _wordSearchResults.value.size + _triviaResults.value.size

    suspend fun resetAll() {
        _wordSearchResults.value = emptyMap()
        _triviaResults.value = emptyMap()
        _totalCoins.value = 0
        dataStore.edit { it.clear() }
    }

    companion object {
        private val WORD_SEARCH_KEY = stringPreferencesKey("wq.wordsearch.results.v2")
        private val TRIVIA_KEY = stringPreferencesKey("wq.trivia.results.v2")
        private val COINS_KEY = intPreferencesKey("wq.coins")

        @Volatile private var instance: ProgressStore? = null

        fun getInstance(context: Context, gameServices: GameServicesReporter = NoOpGameServicesReporter): ProgressStore =
            instance ?: synchronized(this) {
                instance ?: ProgressStore(context, gameServices).also { instance = it }
            }
    }
}
