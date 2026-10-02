package com.mamatiquest.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.mamatiquest.app.services.AdsManager
import com.mamatiquest.app.services.PlayGamesReporter
import com.mamatiquest.app.ui.WordQuestApp

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Mirrors iOS's RootView `.task` block: authenticate with the game
        // services backend right at launch.
        PlayGamesReporter.attach(this)
        requestNotificationPermissionIfNeeded()
        AdsManager.start(this)

        setContent {
            WordQuestApp()
        }
    }

    /** Needed for TriviaActivityNotifier's Live-Activity-equivalent notification to actually show. */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
