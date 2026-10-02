package com.mamatiquest.app.data

import com.mamatiquest.app.models.GridDirection
import com.mamatiquest.app.models.GridPosition
import com.mamatiquest.app.models.PlacedWord

/**
 * Places words into a gridSize x gridSize grid using deterministic RNG, trying
 * all 8 directions with overlap support, then fills remaining cells. Ported
 * faithfully from iOS's WordSearchGenerator.
 */
object WordSearchGenerator {
    private val ALPHABET = ('A'..'Z').toList()

    fun generate(words: List<String>, gridSize: Int, seed: Long): Pair<List<List<Char>>, List<PlacedWord>> {
        val rng = SeededGenerator(seed)
        val grid: Array<Array<Char?>> = Array(gridSize) { arrayOfNulls(gridSize) }
        val placed = mutableListOf<PlacedWord>()

        val sortedWords = words.filter { it.length <= gridSize }.sortedByDescending { it.length }

        for (word in sortedWords) {
            val path = tryPlace(word, grid, gridSize, rng)
            if (path != null) placed.add(PlacedWord(word = word, path = path))
        }

        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (grid[r][c] == null) {
                    grid[r][c] = rng.pick(ALPHABET)
                }
            }
        }

        val finalGrid = grid.map { row -> row.map { it ?: '?' } }
        return finalGrid to placed
    }

    private fun tryPlace(
        word: String,
        grid: Array<Array<Char?>>,
        gridSize: Int,
        rng: SeededGenerator,
    ): List<GridPosition>? {
        val letters = word.toList()
        val maxAttempts = 200

        repeat(maxAttempts) {
            val direction = rng.pick(GridDirection.entries) ?: return null
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
}
