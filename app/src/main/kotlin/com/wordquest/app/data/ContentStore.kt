package com.wordquest.app.data

import android.content.Context
import com.wordquest.app.models.GameTheme
import com.wordquest.app.models.TriviaQuestionData
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Android counterpart to iOS's ContentStore: loads `themes.json` and the
 * per-theme `words_*.json` / `trivia_*.json` files from `assets/content/`
 * (the iOS files are bundled 1:1, byte-for-byte, under that folder).
 */
class ContentStore private constructor(context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val assets = context.applicationContext.assets

    val themes: List<GameTheme> = load("themes")

    private val wordsByTheme: Map<String, List<String>> =
        themes.associate { it.id to load<List<String>>("words_${it.id}") }

    private val questionsByTheme: Map<String, List<TriviaQuestionData>> =
        themes.associate { it.id to load<List<TriviaQuestionData>>("trivia_${it.id}") }

    fun words(themeId: String): List<String> = wordsByTheme[themeId] ?: emptyList()

    fun questions(themeId: String): List<TriviaQuestionData> = questionsByTheme[themeId] ?: emptyList()

    fun theme(id: String): GameTheme = themes.firstOrNull { it.id == id } ?: themes[0]

    private inline fun <reified T> load(filename: String): T {
        val text = assets.open("content/$filename.json").bufferedReader().use { it.readText() }
        return json.decodeFromString(text)
    }

    companion object {
        @Volatile private var instance: ContentStore? = null

        fun getInstance(context: Context): ContentStore =
            instance ?: synchronized(this) {
                instance ?: ContentStore(context).also { instance = it }
            }
    }
}
