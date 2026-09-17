package com.wordquest.app.models

import kotlinx.serialization.Serializable

@Serializable
data class PlayerInfo(val id: String, val name: String)

@Serializable
data class PlayerScore(
    val id: String,
    val name: String,
    val score: Int,
    val correctCount: Int,
)

@Serializable
data class NetworkQuestion(val text: String, val options: List<String>)

/**
 * The wire protocol for local multiplayer trivia. The host is authoritative: it
 * owns the correct answers, paces questions, scores submissions, and broadcasts
 * state to every connected peer over the Nearby Connections API (the Android
 * counterpart to iOS's MultipeerConnectivity — it transparently uses Bluetooth
 * or local Wi-Fi, whichever link is available between devices).
 *
 * Data shapes only in this phase; the actual Nearby Connections service that
 * sends/receives these messages is built in a later phase.
 */
@Serializable
sealed class MultiplayerMessage {
    @Serializable
    data class Hello(val player: PlayerInfo) : MultiplayerMessage()

    @Serializable
    data class LobbyState(
        val players: List<PlayerInfo>,
        val hostId: String,
        val questionCount: Int,
    ) : MultiplayerMessage()

    @Serializable
    data class GameStart(
        val questions: List<NetworkQuestion>,
        val timePerQuestion: Int,
        val theme: GameTheme,
    ) : MultiplayerMessage()

    @Serializable
    data class QuestionStart(val index: Int) : MultiplayerMessage()

    @Serializable
    data class Answer(
        val playerId: String,
        val questionIndex: Int,
        val answerIndex: Int,
        val elapsedMs: Int,
    ) : MultiplayerMessage()

    @Serializable
    data class QuestionResult(
        val index: Int,
        val correctIndex: Int,
        val scores: List<PlayerScore>,
    ) : MultiplayerMessage()

    @Serializable
    data class GameOver(val scores: List<PlayerScore>) : MultiplayerMessage()

    @Serializable
    data class PlayerLeft(val playerId: String) : MultiplayerMessage()
}
