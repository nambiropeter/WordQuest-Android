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
import com.mamatiquest.app.models.TriviaLevel
import com.mamatiquest.app.models.TriviaQuestion
import com.mamatiquest.app.services.GameServicesReporter
import com.mamatiquest.app.services.HapticsManager
import com.mamatiquest.app.services.PlayGamesReporter
import com.mamatiquest.app.services.SoundManager
import com.mamatiquest.app.services.TriviaActivityNotifier
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AnswerState { UNANSWERED, CORRECT, WRONG }

/**
 * Android counterpart to iOS's TriviaViewModel — same scoring/streak/lifeline
 * rules. Also drives [TriviaActivityNotifier], the Android analogue of iOS's
 * Live Activity (`TriviaActivityManager`), at the same lifecycle points: once
 * on question start, once per answer/score change, and once on completion.
 */
class TriviaViewModel(
    application: Application,
    val theme: GameTheme,
    val level: Int,
    private val gameServices: GameServicesReporter = PlayGamesReporter,
) : AndroidViewModel(application) {

    private val contentStore = ContentStore.getInstance(application)
    private val progressStore = ProgressStore.getInstance(application)

    var triviaLevel by mutableStateOf(LevelCatalog.triviaLevel(contentStore, theme, level))
        private set

    var currentIndex by mutableIntStateOf(0)
        private set

    var score by mutableIntStateOf(0)
        private set

    var streak by mutableIntStateOf(0)
        private set

    var bestStreak by mutableIntStateOf(0)
        private set

    var selectedIndex by mutableStateOf<Int?>(null)
        private set

    var answerState by mutableStateOf(AnswerState.UNANSWERED)
        private set

    var eliminatedOptions by mutableStateOf<Set<Int>>(emptySet())
        private set

    var fiftyFiftyUsed by mutableStateOf(false)
        private set

    var timeRemaining by mutableIntStateOf(triviaLevel.timePerQuestion)
        private set

    var isComplete by mutableStateOf(false)
        private set

    var correctCount by mutableIntStateOf(0)
        private set

    private var timerJob: Job? = null
    private var activityNotifierStarted = false

    val currentQuestion: TriviaQuestion get() = triviaLevel.questions[currentIndex]
    val totalQuestions: Int get() = triviaLevel.questions.size
    val isLastQuestion: Boolean get() = currentIndex == totalQuestions - 1

    fun startQuestionTimer() {
        stopTimer()
        timeRemaining = triviaLevel.timePerQuestion
        notifyActivity()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                if (timeRemaining > 0) {
                    timeRemaining--
                    if (timeRemaining <= 3) SoundManager.tick()
                } else {
                    timeExpired()
                    return@launch
                }
            }
        }
    }

    private fun notifyActivity() {
        val context = getApplication<Application>()
        val deadlineMillis = System.currentTimeMillis() + timeRemaining * 1000L
        if (!activityNotifierStarted) {
            activityNotifierStarted = true
            TriviaActivityNotifier.start(context, theme, level, totalQuestions, deadlineMillis)
        } else {
            TriviaActivityNotifier.update(
                context, theme, level, currentIndex, totalQuestions, score, streak, deadlineMillis,
            )
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun timeExpired() {
        if (answerState != AnswerState.UNANSWERED) return
        answerState = AnswerState.WRONG
        streak = 0
        stopTimer()
        HapticsManager.error()
        SoundManager.wrongAnswer()
        notifyActivity()
    }

    fun selectAnswer(index: Int) {
        if (answerState != AnswerState.UNANSWERED) return
        selectedIndex = index
        stopTimer()

        if (index == currentQuestion.correctIndex) {
            answerState = AnswerState.CORRECT
            streak++
            bestStreak = maxOf(bestStreak, streak)
            correctCount++
            val speedBonus = timeRemaining * 5
            val streakBonus = minOf(streak * 10, 100)
            score += currentQuestion.difficulty.points + speedBonus + streakBonus
            HapticsManager.success()
            HapticsManager.playCorrectPulse()
            SoundManager.correctAnswer()
        } else {
            answerState = AnswerState.WRONG
            streak = 0
            HapticsManager.error()
            SoundManager.wrongAnswer()
        }
        notifyActivity()
    }

    fun useFiftyFifty() {
        if (fiftyFiftyUsed || answerState != AnswerState.UNANSWERED) return
        fiftyFiftyUsed = true
        val wrongIndices = currentQuestion.options.indices.filter { it != currentQuestion.correctIndex }
        eliminatedOptions = wrongIndices.shuffled().take(2).toSet()
        HapticsManager.light()
    }

    fun advance() {
        if (isLastQuestion) {
            finish()
            return
        }
        currentIndex++
        selectedIndex = null
        answerState = AnswerState.UNANSWERED
        eliminatedOptions = emptySet()
        startQuestionTimer()
    }

    private fun finish() {
        isComplete = true
        stopTimer()
        HapticsManager.playLevelComplete()
        if (activityNotifierStarted) {
            TriviaActivityNotifier.end(getApplication(), theme, level, score)
            activityNotifierStarted = false
        }

        val stars = computeStars()
        progressStore.recordCompletion(
            level = level,
            mode = GameMode.TRIVIA,
            theme = theme,
            stars = stars,
            score = score,
            timeSeconds = 0,
        )
        if (correctCount == totalQuestions && totalQuestions > 0) {
            gameServices.reportAchievement(GameServicesReporter.AchievementId.PERFECT_TRIVIA)
        }
    }

    fun computeStars(): Int {
        val ratio = if (totalQuestions == 0) 0.0 else correctCount.toDouble() / totalQuestions
        return when {
            ratio >= 0.9 -> 3
            ratio >= 0.6 -> 2
            ratio > 0.0 -> 1
            else -> 0
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (activityNotifierStarted && !isComplete) {
            TriviaActivityNotifier.cancel(getApplication())
        }
    }

    companion object {
        fun factory(application: Application, theme: GameTheme, level: Int) = viewModelFactory {
            initializer { TriviaViewModel(application, theme, level) }
        }
    }
}
