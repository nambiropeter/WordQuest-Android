package com.mamatiquest.app.data

import com.mamatiquest.app.models.GridDirection
import com.mamatiquest.app.models.GridPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSearchGeneratorTest {
    private val words = listOf(
        "LION", "TIGER", "ELEPHANT", "GIRAFFE", "ZEBRA", "PANDA", "KOALA", "KANGAROO",
        "CHEETAH", "LEOPARD", "GORILLA", "MONKEY", "RHINO", "HIPPO",
    )

    @Test
    fun placedWordsSpellCorrectlyAndAppearOnlyOnce() {
        for (band in 0..9) {
            val gridSize = minOf(8 + band, 14)
            for (seed in 0L until 40L) {
                val chosen = SeededGenerator(seed).shuffled(words.filter { it.length <= gridSize }).take(5 + band / 2)
                val (grid, placed) = WordSearchGenerator.generate(chosen, gridSize, seed * 31 + band, band / 9f)

                assertEquals(gridSize, grid.size)
                assertTrue(grid.all { row -> row.size == gridSize && row.all { it in 'A'..'Z' } })
                for (pw in placed) {
                    assertEquals(pw.word, pw.path.map { grid[it.row][it.col] }.joinToString(""))
                    val copies = occurrences(grid, pw.word).filter { it != pw.path && it != pw.path.reversed() }
                    assertTrue("band $band seed $seed: extra ${pw.word} at $copies", copies.isEmpty())
                }
            }
        }
    }

    @Test
    fun sameSeedGivesSamePuzzle() {
        val a = WordSearchGenerator.generate(words.take(8), 12, 42, 0.5f)
        val b = WordSearchGenerator.generate(words.take(8), 12, 42, 0.5f)
        assertEquals(a.first, b.first)
        assertEquals(a.second.map { it.path }, b.second.map { it.path })
    }

    private fun occurrences(grid: List<List<Char>>, word: String): List<List<GridPosition>> {
        val n = grid.size
        return (0 until n).flatMap { r ->
            (0 until n).flatMap { c ->
                GridDirection.entries.mapNotNull { d ->
                    val path = word.indices.map { GridPosition(r + d.dr * it, c + d.dc * it) }
                    path.takeIf { p ->
                        p.all { it.row in 0 until n && it.col in 0 until n } &&
                            p.indices.all { grid[p[it].row][p[it].col] == word[it] }
                    }
                }
            }
        }
    }
}
