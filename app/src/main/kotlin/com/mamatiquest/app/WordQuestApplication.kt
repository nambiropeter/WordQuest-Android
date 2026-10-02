package com.mamatiquest.app

import android.app.Application
import com.mamatiquest.app.data.ContentStore
import com.mamatiquest.app.data.ProgressStore
import com.mamatiquest.app.services.HapticsManager
import com.mamatiquest.app.services.PlayGamesReporter
import com.mamatiquest.app.services.SettingsStore

class WordQuestApplication : Application() {
    lateinit var contentStore: ContentStore
        private set
    lateinit var progressStore: ProgressStore
        private set
    lateinit var settingsStore: SettingsStore
        private set

    override fun onCreate() {
        super.onCreate()
        contentStore = ContentStore.getInstance(this)
        progressStore = ProgressStore.getInstance(this, PlayGamesReporter)
        settingsStore = SettingsStore.getInstance(this)
        HapticsManager.initialize(this)
    }
}
