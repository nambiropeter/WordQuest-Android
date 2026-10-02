package com.mamatiquest.app.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mamatiquest.app.data.ContentStore
import com.mamatiquest.app.data.LevelCatalog
import com.mamatiquest.app.data.ProgressStore
import com.mamatiquest.app.models.GameMode
import com.mamatiquest.app.models.GameTheme
import com.mamatiquest.app.models.GridPosition
import com.mamatiquest.app.services.HapticsManager
import com.mamatiquest.app.services.SoundManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Android counterpart to iOS's WordSearchViewModel — same gameplay/scoring rules. */
class WordSearchViewModel(
    application: Application,
    val theme: GameTheme,
    val level: Int,
) : AndroidViewModel(application) {

    private val contentStore = ContentStore.getInstance(application)
    private val progressStore = ProgressStore.getInstance(application)

    var puzzle by mutableStateOf(LevelCatalog.wordSearchLevel(contentStore, theme, level))
        private set

    var currentSelection by mutableStateOf<List<GridPosition>>(emptyList())
        private set

    var foundPaths by mutableStateOf<List<List<GridPosition>>>(emptyList())
        private set

    /**
     * Letters revealed so far for the word currently being hinted, in
     * spelling order. Each hint extends it by one letter, tracing the word
     * like a match line, and it stays on screen until that word is found.
     */
    var hintedPath by mutableStateOf<List<GridPosition>>(emptyList())
        private set

    var hintsUsed by mutableIntStateOf(0)
        private set

    var hintsRemaining by mutableIntStateOf(puzzle.hintsAllowed)
        private set

    var elapsedSeconds by mutableIntStateOf(0)
        private set

    var isComplete by mutableStateOf(false)
        private set

    var lastFoundWasCorrect by mutableStateOf<Boolean?>(null)
        private set

    private var dragStart: GridPosition? = null
    private var hintedWordIndex: Int? = null
    private var timerJob: Job? = null

    val totalWords: Int get() = puzzle.placedWords.size
    val foundCount: Int get() = puzzle.placedWords.count { it.isFound }

    fun startTimer() {
        stopTimer()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                elapsedSeconds++
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    fun beginSelection(position: GridPosition) {
        dragStart = position
        currentSelection = listOf(position)
    }

    fun updateSelection(position: GridPosition) {
        val start = dragStart ?: return
        currentSelection = straightLine(start, position) ?: listOf(start)
    }

    fun endSelection() {
        val selection = currentSelection
        dragStart = null
        currentSelection = emptyList()
        if (selection.isNotEmpty()) checkSelection(selection)
    }

    private fun checkSelection(selection: List<GridPosition>) {
        val reversed = selection.reversed()
        val index = puzzle.placedWords.indexOfFirst { !it.isFound && (it.path == selection || it.path == reversed) }

        if (index >= 0) {
            val updated = puzzle.placedWords.toMutableList()
            updated[index] = updated[index].copy(isFound = true)
            puzzle = puzzle.copy(placedWords = updated)
            foundPaths = foundPaths + listOf(updated[index].path)
            if (index == hintedWordIndex) {
                hintedWordIndex = null
                hintedPath = emptyList()
            }
            lastFoundWasCorrect = true
            SoundManager.wordFound()
            HapticsManager.success()
            checkCompletion()
        } else {
            lastFoundWasCorrect = false
            HapticsManager.error()
        }
    }

    /**
     * Reveals the next letter of the word being hinted, starting a new
     * unfound word when there isn't one in progress (or it's fully shown).
     */
    fun useHint() {
        if (hintsRemaining <= 0) return
        val current = hintedWordIndex
        if (current != null && hintedPath.size < puzzle.placedWords[current].path.size) {
            hintedPath = puzzle.placedWords[current].path.take(hintedPath.size + 1)
        } else {
            val next = puzzle.placedWords.indices.firstOrNull { !puzzle.placedWords[it].isFound && it != current } ?: return
            hintedWordIndex = next
            hintedPath = listOf(puzzle.placedWords[next].path.first())
        }
        hintsRemaining--
        hintsUsed++
        HapticsManager.light()
    }

    /** Reward for watching an opt-in rewarded ad. */
    fun grantHint() {
        hintsRemaining++
    }

    private fun checkCompletion() {
        if (foundCount != totalWords || totalWords == 0) return
        isComplete = true
        stopTimer()
        SoundManager.levelComplete()
        HapticsManager.playLevelComplete()

        val stars = computeStars()
        progressStore.recordCompletion(
            level = level,
            mode = GameMode.WORD_SEARCH,
            theme = theme,
            stars = stars,
            score = foundCount * 100 + hintsRemaining * 20,
            timeSeconds = elapsedSeconds,
        )
    }

    fun computeStars(): Int {
        val parTime = 20 + totalWords * 15
        return when {
            elapsedSeconds <= parTime && hintsUsed == 0 -> 3
            elapsedSeconds <= parTime * 2 -> 2
            else -> 1
        }
    }

    companion object {
        fun straightLine(start: GridPosition, end: GridPosition): List<GridPosition>? {
            val dr = end.row - start.row
            val dc = end.col - start.col
            if (dr == 0 && dc == 0) return listOf(start)
            if (!(dr == 0 || dc == 0 || abs(dr) == abs(dc))) return null

            val steps = maxOf(abs(dr), abs(dc))
            val stepR = if (dr == 0) 0 else dr / abs(dr)
            val stepC = if (dc == 0) 0 else dc / abs(dc)

            return (0..steps).map { i -> GridPosition(start.row + stepR * i, start.col + stepC * i) }
        }

        fun factory(application: Application, theme: GameTheme, level: Int) = viewModelFactory {
            initializer { WordSearchViewModel(application, theme, level) }
        }
    }
}
