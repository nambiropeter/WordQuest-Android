package com.mamatiquest.app.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
 * The wire protocol for local multiplayer trivia, shared byte-for-byte with
 * iOS's `MultiplayerMessage` so iPhones and Android phones can play together.
 * The host is authoritative: it owns the correct answers, paces questions,
 * scores submissions, and broadcasts state to every connected peer.
 *
 * Each message is a flat JSON object whose `"type"` field names the case
 * (the [SerialName] values below), e.g.
 * `{"type":"answer","playerId":"…","questionIndex":2,"answerIndex":1,"elapsedMs":4200}`.
 * Renaming a case or field here means changing the iOS encoder to match.
 */
@Serializable
sealed class MultiplayerMessage {
    @Serializable
    @SerialName("hello")
    data class Hello(val player: PlayerInfo) : MultiplayerMessage()

    @Serializable
    @SerialName("lobbyState")
    data class LobbyState(
        val players: List<PlayerInfo>,
        val hostId: String,
        val questionCount: Int,
    ) : MultiplayerMessage()

    @Serializable
    @SerialName("gameStart")
    data class GameStart(
        val questions: List<NetworkQuestion>,
        val timePerQuestion: Int,
        val theme: GameTheme,
    ) : MultiplayerMessage()

    @Serializable
    @SerialName("questionStart")
    data class QuestionStart(val index: Int) : MultiplayerMessage()

    @Serializable
    @SerialName("answer")
    data class Answer(
        val playerId: String,
        val questionIndex: Int,
        val answerIndex: Int,
        val elapsedMs: Int,
    ) : MultiplayerMessage()

    @Serializable
    @SerialName("questionResult")
    data class QuestionResult(
        val index: Int,
        val correctIndex: Int,
        val scores: List<PlayerScore>,
    ) : MultiplayerMessage()

    @Serializable
    @SerialName("gameOver")
    data class GameOver(val scores: List<PlayerScore>) : MultiplayerMessage()

    @Serializable
    @SerialName("playerLeft")
    data class PlayerLeft(val playerId: String) : MultiplayerMessage()
}

object WireFormat {
    /** `"type"` is the discriminator iOS writes and reads. */
    val json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
    }
}
