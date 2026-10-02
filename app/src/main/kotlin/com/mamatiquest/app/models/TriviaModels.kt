package com.mamatiquest.app.models

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class TriviaDifficulty {
    easy, medium, hard;

    val points: Int
        get() = when (this) {
            easy -> 100
            medium -> 150
            hard -> 200
        }
}

@Serializable
data class TriviaQuestionData(
    val q: String,
    val options: List<String>,
    val answer: Int,
    val difficulty: TriviaDifficulty,
)

data class TriviaQuestion(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val options: List<String>,
    val correctIndex: Int,
    val difficulty: TriviaDifficulty,
)

data class TriviaLevel(
    val levelIndex: Int,
    val theme: GameTheme,
    val questions: List<TriviaQuestion>,
    val timePerQuestion: Int,
)
