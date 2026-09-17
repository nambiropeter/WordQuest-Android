package com.wordquest.app.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wordquest.app.data.ContentStore
import com.wordquest.app.data.LevelCatalog
import com.wordquest.app.data.ProgressStore
import com.wordquest.app.models.GameMode
import com.wordquest.app.models.GameTheme
import com.wordquest.app.models.GridPosition
import com.wordquest.app.services.HapticsManager
import com.wordquest.app.services.SoundManager
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

    var hintedPosition by mutableStateOf<GridPosition?>(null)
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
        if (selection.size > 1) checkSelection(selection)
    }

    private fun checkSelection(selection: List<GridPosition>) {
        val reversed = selection.reversed()
        val index = puzzle.placedWords.indexOfFirst { !it.isFound && (it.path == selection || it.path == reversed) }

        if (index >= 0) {
            val updated = puzzle.placedWords.toMutableList()
            updated[index] = updated[index].copy(isFound = true)
            puzzle = puzzle.copy(placedWords = updated)
            foundPaths = foundPaths + listOf(updated[index].path)
            lastFoundWasCorrect = true
            SoundManager.wordFound()
            HapticsManager.success()
            checkCompletion()
        } else {
            lastFoundWasCorrect = false
            HapticsManager.error()
        }
    }

    fun useHint() {
        if (hintsRemaining <= 0) return
        val target = puzzle.placedWords.firstOrNull { !it.isFound } ?: return
        hintsRemaining--
        hintedPosition = target.path.first()
        HapticsManager.light()
        viewModelScope.launch {
            delay(1200)
            hintedPosition = null
        }
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
        val usedHints = puzzle.hintsAllowed - hintsRemaining
        return when {
            elapsedSeconds <= parTime && usedHints == 0 -> 3
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
