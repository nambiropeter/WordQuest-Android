package com.wordquest.app.services

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "wq_settings")

enum class AppAppearance {
    SYSTEM, LIGHT, DARK;

    val label: String
        get() = when (this) {
            SYSTEM -> "System"
            LIGHT -> "Light"
            DARK -> "Dark"
        }
}

/**
 * Android counterpart to iOS's SettingsStore. Same keys (translated to
 * DataStore Preferences keys) and same defaults as the Swift original.
 */
class SettingsStore private constructor(context: Context) {
    private val dataStore = context.applicationContext.settingsDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _hapticsEnabled = MutableStateFlow(true)
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    private val _appearance = MutableStateFlow(AppAppearance.SYSTEM)
    val appearance: StateFlow<AppAppearance> = _appearance.asStateFlow()

    /** Governs the small "suggested for you" card on Home. */
    private val _showSuggestions = MutableStateFlow(true)
    val showSuggestions: StateFlow<Boolean> = _showSuggestions.asStateFlow()

    /**
     * Whether the player has opted in to ads personalization. This only
     * reflects intent — the actual permission lives in Android's ad-id /
     * consent flow (the Play Games / UMP counterpart of iOS's App Tracking
     * Transparency), wired up alongside AdMob in a later phase.
     */
    private val _personalizedAdsEnabled = MutableStateFlow(false)
    val personalizedAdsEnabled: StateFlow<Boolean> = _personalizedAdsEnabled.asStateFlow()

    init {
        scope.launch {
            val prefs = dataStore.data.first()
            _soundEnabled.value = prefs[SOUND_KEY] ?: true
            _hapticsEnabled.value = prefs[HAPTICS_KEY] ?: true
            _appearance.value = prefs[APPEARANCE_KEY]?.let { raw ->
                runCatching { AppAppearance.valueOf(raw) }.getOrNull()
            } ?: AppAppearance.SYSTEM
            _showSuggestions.value = prefs[SHOW_SUGGESTIONS_KEY] ?: true
            _personalizedAdsEnabled.value = prefs[PERSONALIZED_ADS_KEY] ?: false
        }
    }

    fun setSoundEnabled(value: Boolean) {
        _soundEnabled.value = value
        scope.launch { dataStore.edit { it[SOUND_KEY] = value } }
    }

    fun setHapticsEnabled(value: Boolean) {
        _hapticsEnabled.value = value
        scope.launch { dataStore.edit { it[HAPTICS_KEY] = value } }
    }

    fun setAppearance(value: AppAppearance) {
        _appearance.value = value
        scope.launch { dataStore.edit { it[APPEARANCE_KEY] = value.name } }
    }

    fun setShowSuggestions(value: Boolean) {
        _showSuggestions.value = value
        scope.launch { dataStore.edit { it[SHOW_SUGGESTIONS_KEY] = value } }
    }

    fun setPersonalizedAdsEnabled(value: Boolean) {
        _personalizedAdsEnabled.value = value
        scope.launch { dataStore.edit { it[PERSONALIZED_ADS_KEY] = value } }
    }

    companion object {
        private val SOUND_KEY = booleanPreferencesKey("wq.settings.sound")
        private val HAPTICS_KEY = booleanPreferencesKey("wq.settings.haptics")
        private val APPEARANCE_KEY = stringPreferencesKey("wq.settings.appearance")
        private val SHOW_SUGGESTIONS_KEY = booleanPreferencesKey("wq.settings.showSuggestions")
        private val PERSONALIZED_ADS_KEY = booleanPreferencesKey("wq.settings.personalizedAds")

        @Volatile private var instance: SettingsStore? = null

        fun getInstance(context: Context): SettingsStore =
            instance ?: synchronized(this) {
                instance ?: SettingsStore(context).also { instance = it }
            }
    }
}
