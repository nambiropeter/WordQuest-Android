package com.mamatiquest.app.data

import com.mamatiquest.app.models.GameMode
import com.mamatiquest.app.models.GameTheme
import com.mamatiquest.app.models.TriviaDifficulty
import com.mamatiquest.app.models.TriviaLevel
import com.mamatiquest.app.models.TriviaQuestion
import com.mamatiquest.app.models.TriviaQuestionData
import com.mamatiquest.app.models.WordSearchPuzzle
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
            difficulty = b / 9f,
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
        if (pool.isEmpty()) {
            return TriviaLevel(levelIndex = level, theme = theme, questions = emptyList(), timePerQuestion = timePerQuestion)
        }

        // A fixed "bag" for this (theme, band), weighted towards the band's
        // target difficulty (matching questions first, then the rest — each
        // half independently shuffled). Every level in the band draws a
        // consecutive, wrapping slice of this same bag, offset by how many
        // questions prior levels in the band have already drawn. That spreads
        // the whole pool evenly across the band's 100 levels instead of every
        // level independently re-rolling the entire pool, which — with pools
        // as small as ~30 questions — made the same questions reappear on
        // almost every level.
        val targetDifficulty = if (b < 3) TriviaDifficulty.easy else if (b < 7) TriviaDifficulty.medium else TriviaDifficulty.hard
        val (targetMatch, rest) = pool.partition { it.difficulty == targetDifficulty }
        val bandSeed = base * 15485863 + b * 27 + 3
        val bag = SeededGenerator(bandSeed).shuffled(targetMatch) + SeededGenerator(bandSeed * 2 + 1).shuffled(rest)

        val effectiveCount = min(questionCount, bag.size)
        val levelInBand = (level - 1) % 100
        val offset = (levelInBand * effectiveCount) % bag.size
        val chosen = (0 until effectiveCount).map { bag[(offset + it) % bag.size] }

        val optionsRng = SeededGenerator(base * 50000017 + level * 97 + 11)
        val questions = chosen.map { data -> buildQuestion(data, optionsRng) }

        return TriviaLevel(levelIndex = level, theme = theme, questions = questions, timePerQuestion = timePerQuestion)
    }

    /**
     * The stored data always lists the correct answer alongside its distractors
     * in a fixed order (usually first), so building [TriviaQuestion.options]
     * straight from [TriviaQuestionData.options] made the correct choice
     * predictable (almost always option A). Shuffling the options — and
     * remapping the correct index to match — fixes that while keeping single
     * player deterministic per (theme, level) via the seeded [rng].
     */
    private fun buildQuestion(data: TriviaQuestionData, rng: SeededGenerator): TriviaQuestion {
        val order = rng.shuffled(data.options.indices.toList())
        return TriviaQuestion(
            text = data.q,
            options = order.map { data.options[it] },
            correctIndex = order.indexOf(data.answer),
            difficulty = data.difficulty,
        )
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
            val order = data.options.indices.shuffled()
            TriviaQuestion(
                text = data.q,
                options = order.map { data.options[it] },
                correctIndex = order.indexOf(data.answer),
                difficulty = data.difficulty,
            )
        }
    }
}
