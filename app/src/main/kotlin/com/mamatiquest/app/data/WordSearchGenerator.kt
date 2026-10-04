package com.mamatiquest.app.data

import com.mamatiquest.app.models.GridDirection
import com.mamatiquest.app.models.GridPosition
import com.mamatiquest.app.models.PlacedWord

/**
 * Places words into a gridSize x gridSize grid using deterministic RNG, trying
 * all 8 directions with overlap support, then fills remaining cells. Ported
 * from iOS's WordSearchGenerator, with extra difficulty on top:
 *
 * - **Direction bias**: as [difficulty] rises, words favor backwards and
 *   diagonal placements over plain left-to-right / top-to-bottom.
 * - **Decoys**: truncated prefixes of the hidden words (e.g. "ELEPHA") are
 *   scattered around so a promising start often leads nowhere.
 * - **Camouflaged filler**: empty cells are mostly drawn from the hidden
 *   words' own letters instead of uniform A–Z, so words don't stand out
 *   against a sea of Q/X/Z.
 *
 * Selections are matched by exact path, so an accidental second copy of a
 * word would be rejected as wrong. [removeAccidentalCopies] re-rolls filler
 * until every word appears only where it was placed.
 */
object WordSearchGenerator {
    private val ALPHABET = ('A'..'Z').toList()
    private val EASY_DIRECTIONS = setOf(GridDirection.RIGHT, GridDirection.DOWN)

    /** @param difficulty 0.0 (easiest) to 1.0 (hardest). */
    fun generate(
        words: List<String>,
        gridSize: Int,
        seed: Long,
        difficulty: Float = 0f,
    ): Pair<List<List<Char>>, List<PlacedWord>> {
        val d = difficulty.coerceIn(0f, 1f)
        val rng = SeededGenerator(seed)
        val grid: Array<Array<Char?>> = Array(gridSize) { arrayOfNulls(gridSize) }
        val placed = mutableListOf<PlacedWord>()

        val sortedWords = words.filter { it.length <= gridSize }.sortedByDescending { it.length }
        val directions = weightedDirections(d)

        for (word in sortedWords) {
            val path = tryPlace(word, grid, gridSize, rng, directions)
            if (path != null) placed.add(PlacedWord(word = word, path = path))
        }

        placeDecoys(placed, grid, gridSize, rng, d)

        // Letters of the hidden words, with repeats, so common letters in the
        // words are proportionally common in the filler too.
        val wordLetters = placed.flatMap { it.word.toList() }
        val camouflage = 0.5f + 0.4f * d
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (grid[r][c] == null) {
                    val fromWords = wordLetters.isNotEmpty() && rng.nextInt(1000) < camouflage * 1000
                    grid[r][c] = rng.pick(if (fromWords) wordLetters else ALPHABET)
                }
            }
        }

        val finalGrid = Array(gridSize) { r -> CharArray(gridSize) { c -> grid[r][c] ?: '?' } }
        removeAccidentalCopies(finalGrid, placed, rng)
        return finalGrid.map { it.toList() } to placed
    }

    /**
     * Each easy direction keeps a weight of 1 at difficulty 0 (uniform, as
     * before) and drops to 0.2 at difficulty 1, while the six harder
     * directions stay at 1 — i.e. ~25% easy placements down to ~6%.
     */
    private fun weightedDirections(difficulty: Float): List<GridDirection> {
        val easyWeight = (5 - 4 * difficulty).toInt().coerceAtLeast(1) // 5 -> 1
        return GridDirection.entries.flatMap { dir ->
            List(if (dir in EASY_DIRECTIONS) easyWeight else 5) { dir }
        }
    }

    private fun tryPlace(
        word: String,
        grid: Array<Array<Char?>>,
        gridSize: Int,
        rng: SeededGenerator,
        directions: List<GridDirection>,
    ): List<GridPosition>? {
        val letters = word.toList()
        val maxAttempts = 200

        repeat(maxAttempts) {
            val direction = rng.pick(directions) ?: return null
            val startRow = rng.nextInt(gridSize)
            val startCol = rng.nextInt(gridSize)

            val endRow = startRow + direction.dr * (letters.size - 1)
            val endCol = startCol + direction.dc * (letters.size - 1)
            if (endRow !in 0 until gridSize || endCol !in 0 until gridSize) return@repeat

            val positions = mutableListOf<GridPosition>()
            var fits = true
            for (i in letters.indices) {
                val r = startRow + direction.dr * i
                val c = startCol + direction.dc * i
                val existing = grid[r][c]
                if (existing != null && existing != letters[i]) {
                    fits = false
                    break
                }
                positions.add(GridPosition(r, c))
            }

            if (fits) {
                positions.forEachIndexed { i, pos -> grid[pos.row][pos.col] = letters[i] }
                return positions
            }
        }
        return null
    }

    /**
     * Writes word prefixes that stop one or two letters short, in any
     * direction. Only placed where they fit (empty or matching cells), so the
     * real words are never disturbed.
     */
    private fun placeDecoys(
        placed: List<PlacedWord>,
        grid: Array<Array<Char?>>,
        gridSize: Int,
        rng: SeededGenerator,
        difficulty: Float,
    ) {
        val candidates = placed.map { it.word }.filter { it.length >= 4 }
        if (candidates.isEmpty()) return
        val decoyCount = (placed.size * (0.4f + 0.6f * difficulty)).toInt()
        repeat(decoyCount) {
            val word = rng.pick(candidates) ?: return
            val length = word.length - 1 - rng.nextInt(2) // drop 1–2 letters
            tryPlace(word.take(length), grid, gridSize, rng, GridDirection.entries)
        }
    }

    /**
     * Finds every straight-line occurrence of each word that isn't its placed
     * path and re-rolls one cell of it not owned by a real word. Repeats until
     * clean (bounded, in case an occurrence lies entirely on real words — e.g.
     * "CAT" inside "CATFISH" — which can't be fixed by filler changes).
     */
    private fun removeAccidentalCopies(grid: Array<CharArray>, placed: List<PlacedWord>, rng: SeededGenerator) {
        val locked = placed.flatMap { it.path }.toSet()
        repeat(50) {
            var changed = false
            for (pw in placed) {
                for (copy in occurrences(grid, pw.word)) {
                    if (copy == pw.path || copy == pw.path.reversed()) continue
                    val free = copy.filter { it !in locked }
                    val cell = rng.pick(free) ?: continue
                    val current = grid[cell.row][cell.col]
                    grid[cell.row][cell.col] = rng.pick(ALPHABET.filter { it != current })!!
                    changed = true
                }
            }
            if (!changed) return
        }
    }

    private fun occurrences(grid: Array<CharArray>, word: String): List<List<GridPosition>> {
        val size = grid.size
        val result = mutableListOf<List<GridPosition>>()
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (grid[r][c] != word[0]) continue
                for (dir in GridDirection.entries) {
                    val endR = r + dir.dr * (word.length - 1)
                    val endC = c + dir.dc * (word.length - 1)
                    if (endR !in 0 until size || endC !in 0 until size) continue
                    if (word.indices.all { i -> grid[r + dir.dr * i][c + dir.dc * i] == word[i] }) {
                        result.add(word.indices.map { i -> GridPosition(r + dir.dr * i, c + dir.dc * i) })
                    }
                }
            }
        }
        return result
    }
}
