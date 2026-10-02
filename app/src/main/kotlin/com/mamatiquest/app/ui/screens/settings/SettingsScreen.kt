package com.mamatiquest.app.ui.screens.settings

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mamatiquest.app.BuildConfig
import com.mamatiquest.app.data.ProgressStore
import com.mamatiquest.app.services.AdsManager
import com.mamatiquest.app.services.AppAppearance
import com.mamatiquest.app.services.SettingsStore
import kotlinx.coroutines.launch

/** Android counterpart to iOS's `SettingsView`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore.getInstance(context) }
    val progressStore = remember { ProgressStore.getInstance(context) }
    val scope = rememberCoroutineScope()
    var showResetConfirmation by remember { mutableStateOf(false) }

    val soundEnabled by settingsStore.soundEnabled.collectAsState()
    val hapticsEnabled by settingsStore.hapticsEnabled.collectAsState()
    val privacyOptionsRequired by AdsManager.privacyOptionsRequired.collectAsState()
    val appearance by settingsStore.appearance.collectAsState()
    val showSuggestions by settingsStore.showSuggestions.collectAsState()

    val wordSearchResults by progressStore.wordSearchResults.collectAsState()
    val triviaResults by progressStore.triviaResults.collectAsState()
    val totalCoins by progressStore.totalCoins.collectAsState()
    val totalStars = wordSearchResults.values.sumOf { it.stars } + triviaResults.values.sumOf { it.stars }
    val levelsCompleted = wordSearchResults.size + triviaResults.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            SettingsSection(title = "Appearance") {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    AppAppearance.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = appearance == option,
                            onClick = { settingsStore.setAppearance(option) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = AppAppearance.entries.size),
                        ) {
                            Text(option.label)
                        }
                    }
                }
            }

            SettingsSection(title = "Preferences") {
                ToggleRow(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    label = "Sound Effects",
                    checked = soundEnabled,
                    onCheckedChange = { settingsStore.setSoundEnabled(it) },
                )
                ToggleRow(
                    icon = Icons.Filled.Vibration,
                    label = "Haptic Feedback",
                    checked = hapticsEnabled,
                    onCheckedChange = { settingsStore.setHapticsEnabled(it) },
                )
            }

            SettingsSection(
                title = "Suggestions",
                footer = "Chosen from what you already play, right on your device — nothing is sent anywhere.",
            ) {
                ToggleRow(
                    icon = Icons.Filled.Sensors,
                    label = "Suggested For You",
                    checked = showSuggestions,
                    onCheckedChange = { settingsStore.setShowSuggestions(it) },
                )
            }

            SettingsSection(title = "Progress") {
                LabeledRow("Total Stars", "$totalStars")
                LabeledRow("Coins", "$totalCoins")
                LabeledRow("Levels Completed", "$levelsCompleted")
            }

            // Only where the law (e.g. GDPR) requires a way to change the ad consent choice.
            if (privacyOptionsRequired) {
                SettingsSection(title = "Ads") {
                    TextButton(onClick = { (context as? Activity)?.let(AdsManager::showPrivacyOptions) }) {
                        Text("Ad Privacy Choices")
                    }
                }
            }

            if (BuildConfig.DEBUG) {
                SettingsSection(title = "Debug") {
                    TextButton(onClick = { showResetConfirmation = true }) {
                        Text("Reset All Progress", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            SettingsSection(title = "About") {
                LabeledRow("Version", BuildConfig.VERSION_NAME)
                Text(
                    "WordQuest features 1,000 Word Search levels and 1,000 Trivia levels across 8 themes: " +
                        "Animals, Geography, Movies & TV, Science, Sports, Food & Cooking, History, and Music.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Reset all progress?") },
            text = { Text("This permanently deletes every level's stars, scores, and coins. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirmation = false
                    scope.launch { progressStore.resetAll() }
                }) {
                    Text("Reset", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun SettingsSection(title: String, footer: String? = null, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), contentColor = MaterialTheme.colorScheme.onSurfaceVariant) {
            Column(modifier = Modifier.padding(4.dp)) { content() }
        }
        if (footer != null) {
            Text(footer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun LabeledRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
