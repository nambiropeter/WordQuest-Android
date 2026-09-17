package com.wordquest.app.data

import com.wordquest.app.models.GameMode
import com.wordquest.app.models.GameTheme
import com.wordquest.app.models.TriviaDifficulty
import com.wordquest.app.models.TriviaLevel
import com.wordquest.app.models.TriviaQuestion
import com.wordquest.app.models.WordSearchPuzzle
import kotlin.math.abs
import kotlin.math.min

/**
 * Produces 1000 deterministic, distinct levels per theme, per game mode, by
 * combining curated per-theme content pools with a seeded generator keyed on
 * theme + level index — ported from iOS's LevelCatalog. The same (theme,
 * level) always yields the same puzzle, but each level draws a different
 * word/question subset, grid size, and difficulty band as you progress.
 *
 * [themeSeedBase] uses Kotlin/Java's `String.hashCode()`, which — unlike
 * Swift's `String.hashValue` (randomized per process launch by design) — is
 * specified to be stable across runs. iOS's own seed is therefore *not*
 * actually reproducible across launches despite its doc comment; Android's
 * version here is, which is the behavior the original evidently intended.
 */
object LevelCatalog {
    const val TOTAL_LEVELS = GameMode.LEVEL_COUNT

    private fun band(level: Int): Int = minOf((level - 1) / 100, 9) // 0...9

    private fun themeSeedBase(theme: GameTheme): Long = abs(theme.id.hashCode() % 1_000_000).toLong()

    fun wordSearchLevel(contentStore: ContentStore, theme: GameTheme, level: Int): WordSearchPuzzle {
        val b = band(level)
        val gridSize = min(8 + b, 14)
        val wordCount = min(5 + b / 2, 10)
        val hintsAllowed = maxOf(1, 3 - b / 4)
        val base = themeSeedBase(theme)

        val pool = contentStore.words(theme.id).filter { it.length <= gridSize }
        val rng = SeededGenerator(base * 7919 + level * 13 + 1)
        val selected = rng.shuffled(pool).take(wordCount)

        val (letters, placed) = WordSearchGenerator.generate(
            words = selected,
            gridSize = gridSize,
            seed = base * 104729 + level * 3 + 2,
        )

        return WordSearchPuzzle(
            levelIndex = level,
            theme = theme,
            gridSize = gridSize,
            letters = letters,
            placedWords = placed,
            hintsAllowed = hintsAllowed,
        )
    }

    fun triviaLevel(contentStore: ContentStore, theme: GameTheme, level: Int): TriviaLevel {
        val b = band(level)
        val questionCount = min(5 + b / 3, 8)
        val timePerQuestion = maxOf(10, 20 - b)
        val base = themeSeedBase(theme)

        val pool = contentStore.questions(theme.id)
        val rng = SeededGenerator(base * 15485863 + level * 27 + 3)

        val targetDifficulty = if (b < 3) TriviaDifficulty.easy else if (b < 7) TriviaDifficulty.medium else TriviaDifficulty.hard
        val weighted = pool.sortedByDescending { it.difficulty == targetDifficulty }
        val chosen = rng.shuffled(weighted).take(questionCount)

        val questions = chosen.map { data ->
            TriviaQuestion(text = data.q, options = data.options, correctIndex = data.answer, difficulty = data.difficulty)
        }

        return TriviaLevel(levelIndex = level, theme = theme, questions = questions, timePerQuestion = timePerQuestion)
    }

    /**
     * Pulls a genuinely random (non-seeded) set of questions across one theme,
     * or mixed across all themes when [theme] is null. Used for local multiplayer.
     */
    fun randomQuestions(contentStore: ContentStore, theme: GameTheme?, count: Int): List<TriviaQuestion> {
        val pool = if (theme != null) {
            contentStore.questions(theme.id)
        } else {
            contentStore.themes.flatMap { contentStore.questions(it.id) }
        }
        return pool.shuffled().take(count).map { data ->
            TriviaQuestion(text = data.q, options = data.options, correctIndex = data.answer, difficulty = data.difficulty)
        }
    }
}
