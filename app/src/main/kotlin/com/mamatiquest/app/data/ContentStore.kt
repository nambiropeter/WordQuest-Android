package com.mamatiquest.app.data

import android.content.Context
import android.util.Log
import com.mamatiquest.app.models.GameTheme
import com.mamatiquest.app.models.TriviaQuestionData
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Android counterpart to iOS's ContentStore: loads `themes.json` and the
 * per-theme `words_*.json` / `trivia_*.json` files from `assets/content/`
 * (the iOS files are bundled 1:1, byte-for-byte, under that folder).
 *
 * Every load is defensive: a missing/malformed content file (e.g. a bad
 * content push) degrades to an empty list instead of crashing the app on
 * first launch, since [WordSearchViewModel] and friends construct this
 * eagerly from `init`.
 */
class ContentStore private constructor(context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val assets = context.applicationContext.assets

    val themes: List<GameTheme> = load("themes", emptyList())

    private val wordsByTheme: Map<String, List<String>> =
        themes.associate { it.id to load("words_${it.id}", emptyList<String>()) }

    private val questionsByTheme: Map<String, List<TriviaQuestionData>> =
        themes.associate { it.id to load("trivia_${it.id}", emptyList<TriviaQuestionData>()) }

    fun words(themeId: String): List<String> = wordsByTheme[themeId] ?: emptyList()

    fun questions(themeId: String): List<TriviaQuestionData> = questionsByTheme[themeId] ?: emptyList()

    fun theme(id: String): GameTheme = themes.firstOrNull { it.id == id } ?: themes.firstOrNull() ?: FALLBACK_THEME

    private inline fun <reified T> load(filename: String, fallback: T): T = runCatching {
        val text = assets.open("content/$filename.json").bufferedReader().use { it.readText() }
        json.decodeFromString<T>(text)
    }.getOrElse {
        Log.e("ContentStore", "Failed to load content/$filename.json, falling back", it)
        fallback
    }

    companion object {
        /** Used only if `themes.json` is missing/corrupt/empty, so screens that assume a non-empty theme list don't crash. */
        private val FALLBACK_THEME = GameTheme(
            id = "mixed",
            name = "Mixed",
            icon = "sparkles",
            colorPrimary = "#7209B7",
            colorSecondary = "#EF476F",
        )

        @Volatile private var instance: ContentStore? = null

        fun getInstance(context: Context): ContentStore =
            instance ?: synchronized(this) {
                instance ?: ContentStore(context).also { instance = it }
            }
    }
}
