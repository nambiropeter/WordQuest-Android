package com.mamatiquest.app.viewmodels

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mamatiquest.app.data.ContentStore
import com.mamatiquest.app.data.LevelCatalog
import com.mamatiquest.app.models.GameTheme
import com.mamatiquest.app.models.MultiplayerMessage
import com.mamatiquest.app.models.NetworkQuestion
import com.mamatiquest.app.models.PlayerInfo
import com.mamatiquest.app.models.PlayerScore
import com.mamatiquest.app.models.TriviaQuestion
import com.mamatiquest.app.services.GameServicesReporter
import com.mamatiquest.app.services.LocalMultiplayerService
import com.mamatiquest.app.services.PlayGamesReporter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

enum class MultiplayerRole { UNDECIDED, HOST, CLIENT }
enum class MultiplayerPhase { ROLE_SELECT, BROWSING, LOBBY, PLAYING, RESULTS }

/**
 * Android counterpart to iOS's `MultiplayerViewModel` — same host-authoritative
 * wire protocol and scoring rules, running over [LocalMultiplayerService]
 * (Bonjour discovery + TCP on the local network), which iOS speaks too.
 */
class MultiplayerViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("wq_player", Context.MODE_PRIVATE)
    private val contentStore = ContentStore.getInstance(application)
    private val gameServices: GameServicesReporter = PlayGamesReporter

    val myId: String = prefs.getString(KEY_PLAYER_ID, null) ?: UUID.randomUUID().toString().also {
        prefs.edit().putString(KEY_PLAYER_ID, it).apply()
    }

    var phase by mutableStateOf(MultiplayerPhase.ROLE_SELECT)
        private set
    var role by mutableStateOf(MultiplayerRole.UNDECIDED)
        private set

    // Android has no direct "device name" API without extra permissions/APIs;
    // Build.MODEL is the closest lightweight analogue to iOS's UIDevice.current.name.
    var playerName by mutableStateOf(prefs.getString(KEY_PLAYER_NAME, null) ?: Build.MODEL ?: "Player")
        private set

    var players by mutableStateOf<List<PlayerInfo>>(emptyList())
        private set
    var discoveredHosts by mutableStateOf<List<LocalMultiplayerService.DiscoveredHost>>(emptyList())
        private set
    var connectingToHostId by mutableStateOf<String?>(null)

    var selectedTheme by mutableStateOf<GameTheme?>(null)
    var questionCount by mutableIntStateOf(8)

    var questions by mutableStateOf<List<NetworkQuestion>>(emptyList())
        private set
    var currentIndex by mutableIntStateOf(0)
        private set
    var timePerQuestion by mutableIntStateOf(15)
        private set
    var timeRemaining by mutableIntStateOf(15)
        private set
    var selectedAnswer by mutableStateOf<Int?>(null)
        private set
    var hasAnswered by mutableStateOf(false)
        private set
    var revealedCorrectIndex by mutableStateOf<Int?>(null)
        private set
    var scores by mutableStateOf<List<PlayerScore>>(emptyList())
        private set
    var themeForGame by mutableStateOf<GameTheme?>(null)
        private set

    private var service: LocalMultiplayerService? = null
    private var fullQuestions: List<TriviaQuestion> = emptyList()
    private val answersThisQuestion = mutableMapOf<String, Pair<Int, Int>>()
    private var questionStartedAtMs = 0L
    private var timerJob: Job? = null
    private val cumulativeScores = mutableMapOf<String, PlayerScore>()
    private val endpointIdToPlayerId = mutableMapOf<String, String>()
    private var isResolvingQuestion = false

    fun updatePlayerName(name: String) {
        val trimmed = name.trim()
        playerName = trimmed.ifEmpty { "Player" }
        prefs.edit().putString(KEY_PLAYER_NAME, playerName).apply()
    }

    private fun setupService() {
        val s = LocalMultiplayerService(getApplication(), "$playerName#${myId.take(4)}")
        s.onPeerConnected = { endpointId -> handlePeerConnected(endpointId) }
        s.onPeerDisconnected = { endpointId -> handlePeerDisconnected(endpointId) }
        s.onReceive = { message, endpointId -> handleMessage(message, endpointId) }
        service = s
    }

    // region Host

    fun startHosting() {
        role = MultiplayerRole.HOST
        setupService()
        service?.startHosting()
        players = listOf(PlayerInfo(myId, playerName))
        phase = MultiplayerPhase.LOBBY
    }

    fun startGame() {
        val svc = service ?: return
        if (role != MultiplayerRole.HOST) return
        val theme = selectedTheme ?: MIXED_THEME
        val qs = LevelCatalog.randomQuestions(contentStore, selectedTheme, questionCount)
        if (qs.isEmpty()) return
        fullQuestions = qs
        themeForGame = theme
        questions = qs.map { NetworkQuestion(it.text, it.options) }
        timePerQuestion = 15
        cumulativeScores.clear()
        players.forEach { cumulativeScores[it.id] = PlayerScore(it.id, it.name, 0, 0) }

        svc.send(MultiplayerMessage.GameStart(questions, timePerQuestion, theme))
        phase = MultiplayerPhase.PLAYING
        beginQuestion(0)
    }

    private fun beginQuestion(index: Int) {
        isResolvingQuestion = false
        currentIndex = index
        selectedAnswer = null
        hasAnswered = false
        revealedCorrectIndex = null
        answersThisQuestion.clear()
        questionStartedAtMs = System.currentTimeMillis()
        timeRemaining = timePerQuestion
        service?.send(MultiplayerMessage.QuestionStart(index))
        startLocalTimer()
    }

    private fun startLocalTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (timeRemaining > 0) {
                    timeRemaining--
                } else {
                    if (role == MultiplayerRole.HOST) finishQuestion()
                    return@launch
                }
            }
        }
    }

    private fun finishQuestion() {
        if (role != MultiplayerRole.HOST || currentIndex >= fullQuestions.size) return
        if (isResolvingQuestion) return
        isResolvingQuestion = true
        timerJob?.cancel()
        val correctIndex = fullQuestions[currentIndex].correctIndex
        val points = fullQuestions[currentIndex].difficulty.points

        for (player in players) {
            val current = cumulativeScores[player.id] ?: continue
            val answer = answersThisQuestion[player.id]
            cumulativeScores[player.id] = if (answer != null && answer.first == correctIndex) {
                val remainingMs = maxOf(0, timePerQuestion * 1000 - answer.second)
                current.copy(score = current.score + points + (remainingMs / 1000) * 5, correctCount = current.correctCount + 1)
            } else {
                current
            }
        }

        val scoresArray = cumulativeScores.values.sortedByDescending { it.score }
        scores = scoresArray
        revealedCorrectIndex = correctIndex
        service?.send(MultiplayerMessage.QuestionResult(currentIndex, correctIndex, scoresArray))

        viewModelScope.launch {
            delay(3000)
            val next = currentIndex + 1
            if (next >= fullQuestions.size) {
                service?.send(MultiplayerMessage.GameOver(scoresArray))
                phase = MultiplayerPhase.RESULTS
                checkChampionAchievement(scoresArray)
            } else {
                beginQuestion(next)
            }
        }
    }

    private fun hostReceivedAnswer(playerId: String, questionIndex: Int, answerIndex: Int) {
        if (role != MultiplayerRole.HOST || questionIndex != currentIndex) return
        // Elapsed time is derived from the host's own receipt clock rather than trusting
        // the peer-reported value, since a modified client could otherwise always claim
        // elapsedMs = 0 for the maximum speed bonus every question.
        val elapsedMs = (System.currentTimeMillis() - questionStartedAtMs).toInt().coerceIn(0, timePerQuestion * 1000)
        answersThisQuestion[playerId] = answerIndex to elapsedMs
        if (answersThisQuestion.size >= players.size) finishQuestion()
    }

    // endregion

    // region Client

    fun startBrowsing() {
        role = MultiplayerRole.CLIENT
        setupService()
        service?.startDiscovery()
        phase = MultiplayerPhase.BROWSING
        viewModelScope.launch {
            service?.discoveredHosts?.collect { discoveredHosts = it }
        }
    }

    fun joinHost(host: LocalMultiplayerService.DiscoveredHost) {
        connectingToHostId = host.endpointId
        service?.requestConnection(host.endpointId)
    }

    // endregion

    // region Shared

    fun selectAnswer(index: Int) {
        if (hasAnswered) return
        hasAnswered = true
        selectedAnswer = index
        if (role == MultiplayerRole.HOST) {
            hostReceivedAnswer(myId, currentIndex, index)
        } else {
            val elapsedMs = (System.currentTimeMillis() - questionStartedAtMs).toInt()
            service?.send(MultiplayerMessage.Answer(myId, currentIndex, index, elapsedMs))
        }
    }

    private fun handlePeerConnected(endpointId: String) {
        service?.send(MultiplayerMessage.Hello(PlayerInfo(myId, playerName)), to = listOf(endpointId))
        if (role == MultiplayerRole.HOST) broadcastLobby()
    }

    private fun handlePeerDisconnected(endpointId: String) {
        val id = endpointIdToPlayerId[endpointId] ?: return
        players = players.filterNot { it.id == id }
        endpointIdToPlayerId.remove(endpointId)
        if (role == MultiplayerRole.HOST) broadcastLobby()
    }

    private fun broadcastLobby() {
        service?.send(MultiplayerMessage.LobbyState(players, myId, questionCount))
    }

    private fun handleMessage(message: MultiplayerMessage, endpointId: String) {
        when (message) {
            is MultiplayerMessage.Hello -> {
                endpointIdToPlayerId[endpointId] = message.player.id
                if (role == MultiplayerRole.HOST && players.none { it.id == message.player.id }) {
                    players = players + message.player
                    // Defensive: if this Hello arrives after startGame() already snapshotted
                    // cumulativeScores, seed an entry so a mid-game (re)join isn't silently unscored.
                    cumulativeScores.putIfAbsent(message.player.id, PlayerScore(message.player.id, message.player.name, 0, 0))
                    broadcastLobby()
                }
            }
            is MultiplayerMessage.LobbyState -> {
                if (role != MultiplayerRole.CLIENT) return
                players = message.players
                questionCount = message.questionCount
                phase = MultiplayerPhase.LOBBY
            }
            is MultiplayerMessage.GameStart -> {
                questions = message.questions
                timePerQuestion = message.timePerQuestion
                themeForGame = message.theme
                phase = MultiplayerPhase.PLAYING
            }
            is MultiplayerMessage.QuestionStart -> {
                currentIndex = message.index
                selectedAnswer = null
                hasAnswered = false
                revealedCorrectIndex = null
                questionStartedAtMs = System.currentTimeMillis()
                timeRemaining = timePerQuestion
                startLocalTimer()
            }
            is MultiplayerMessage.Answer -> {
                hostReceivedAnswer(message.playerId, message.questionIndex, message.answerIndex)
            }
            is MultiplayerMessage.QuestionResult -> {
                if (message.index != currentIndex) return
                revealedCorrectIndex = message.correctIndex
                scores = message.scores
            }
            is MultiplayerMessage.GameOver -> {
                scores = message.scores
                phase = MultiplayerPhase.RESULTS
                checkChampionAchievement(message.scores)
            }
            is MultiplayerMessage.PlayerLeft -> {
                players = players.filterNot { it.id == message.playerId }
            }
        }
    }

    private fun checkChampionAchievement(finalScores: List<PlayerScore>) {
        if (finalScores.size > 1 && finalScores.first().id == myId) {
            gameServices.reportAchievement(GameServicesReporter.AchievementId.MULTIPLAYER_CHAMPION)
        }
    }

    val isHost: Boolean get() = role == MultiplayerRole.HOST
    val myScore: PlayerScore? get() = scores.firstOrNull { it.id == myId }
    val myRank: Int? get() = scores.indexOfFirst { it.id == myId }.takeIf { it >= 0 }?.plus(1)
    val currentQuestion: NetworkQuestion? get() = questions.getOrNull(currentIndex)
    val totalQuestions: Int get() = questions.size
    val connectedCount: Int get() = players.size

    fun leaveGame() {
        timerJob?.cancel()
        service?.disconnect()
        service = null
        phase = MultiplayerPhase.ROLE_SELECT
        role = MultiplayerRole.UNDECIDED
        players = emptyList()
        scores = emptyList()
        questions = emptyList()
        discoveredHosts = emptyList()
        endpointIdToPlayerId.clear()
        answersThisQuestion.clear()
        cumulativeScores.clear()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        service?.disconnect()
    }

    companion object {
        private const val KEY_PLAYER_ID = "wq.player.id"
        private const val KEY_PLAYER_NAME = "wq.player.name"
        val MIXED_THEME = GameTheme(id = "mixed", name = "Mixed", icon = "sparkles", colorPrimary = "#7209B7", colorSecondary = "#EF476F")
    }
}
