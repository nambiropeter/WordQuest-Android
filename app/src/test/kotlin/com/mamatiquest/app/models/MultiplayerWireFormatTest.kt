package com.mamatiquest.app.models

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks the local multiplayer wire format to what the iOS app sends. Each
 * fixture line is verbatim output of iOS's `JSONEncoder` (keys sorted) for
 * `MultiplayerMessage`; if one of these stops decoding, iPhones and Android
 * phones can no longer play together.
 */
class MultiplayerWireFormatTest {
    private val theme = GameTheme("animals", "Animals", "pawprint", "#06D6A0", "#118AB2")
    private val scores = listOf(PlayerScore("a", "Ann", 120, 3))

    private val iosLines = listOf(
        """{"player":{"id":"a","name":"Ann"},"type":"hello"}""" to
            MultiplayerMessage.Hello(PlayerInfo("a", "Ann")),
        """{"hostId":"a","players":[{"id":"a","name":"Ann"}],"questionCount":8,"type":"lobbyState"}""" to
            MultiplayerMessage.LobbyState(listOf(PlayerInfo("a", "Ann")), "a", 8),
        """{"questions":[{"options":["1","2","3","4"],"text":"Q?"}],"theme":{"colorPrimary":"#06D6A0","colorSecondary":"#118AB2","icon":"pawprint","id":"animals","name":"Animals"},"timePerQuestion":15,"type":"gameStart"}""" to
            MultiplayerMessage.GameStart(listOf(NetworkQuestion("Q?", listOf("1", "2", "3", "4"))), 15, theme),
        """{"index":2,"type":"questionStart"}""" to
            MultiplayerMessage.QuestionStart(2),
        """{"answerIndex":1,"elapsedMs":4200,"playerId":"b","questionIndex":2,"type":"answer"}""" to
            MultiplayerMessage.Answer("b", 2, 1, 4200),
        """{"correctIndex":1,"index":2,"scores":[{"correctCount":3,"id":"a","name":"Ann","score":120}],"type":"questionResult"}""" to
            MultiplayerMessage.QuestionResult(2, 1, scores),
        """{"scores":[{"correctCount":3,"id":"a","name":"Ann","score":120}],"type":"gameOver"}""" to
            MultiplayerMessage.GameOver(scores),
        """{"playerId":"b","type":"playerLeft"}""" to
            MultiplayerMessage.PlayerLeft("b"),
    )

    @Test
    fun decodesEveryMessageTheIosAppSends() {
        for ((line, expected) in iosLines) {
            assertEquals(line, expected, WireFormat.json.decodeFromString(MultiplayerMessage.serializer(), line))
        }
    }

    @Test
    fun encodesWithTheTypeDiscriminatorIosExpects() {
        for ((_, message) in iosLines) {
            val encoded = WireFormat.json.encodeToString(MultiplayerMessage.serializer(), message)
            val roundTripped = WireFormat.json.decodeFromString(MultiplayerMessage.serializer(), encoded)
            assertEquals(message, roundTripped)
            println(encoded)
        }
    }
}
