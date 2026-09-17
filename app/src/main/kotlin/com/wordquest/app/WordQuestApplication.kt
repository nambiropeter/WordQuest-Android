package com.wordquest.app

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.wordquest.app.data.ContentStore
import com.wordquest.app.data.ProgressStore
import com.wordquest.app.services.HapticsManager
import com.wordquest.app.services.PlayGamesReporter
import com.wordquest.app.services.SettingsStore

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
        MobileAds.initialize(this)
    }
}
