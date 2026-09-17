package com.wordquest.app.models

import java.util.UUID

enum class GridDirection(val dr: Int, val dc: Int) {
    RIGHT(0, 1),
    LEFT(0, -1),
    DOWN(1, 0),
    UP(-1, 0),
    DOWN_RIGHT(1, 1),
    DOWN_LEFT(1, -1),
    UP_RIGHT(-1, 1),
    UP_LEFT(-1, -1),
}

data class GridPosition(val row: Int, val col: Int)

data class PlacedWord(
    val id: String = UUID.randomUUID().toString(),
    val word: String,
    val path: List<GridPosition>,
    val isFound: Boolean = false,
)

data class WordSearchPuzzle(
    val levelIndex: Int,
    val theme: GameTheme,
    val gridSize: Int,
    val letters: List<List<Char>>,
    val placedWords: List<PlacedWord>,
    val hintsAllowed: Int,
)
